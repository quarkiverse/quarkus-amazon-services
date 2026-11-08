package io.quarkiverse.amazon.appconfigdata.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.amazon.appconfigdata.runtime.AppConfigDataProvider.Profile;
import io.quarkiverse.amazon.common.config.FetchFailure;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.model.AppConfigDataException;
import software.amazon.awssdk.services.appconfigdata.model.BadRequestDetails;
import software.amazon.awssdk.services.appconfigdata.model.BadRequestException;
import software.amazon.awssdk.services.appconfigdata.model.BadRequestReason;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationRequest;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationResponse;
import software.amazon.awssdk.services.appconfigdata.model.InvalidParameterDetail;
import software.amazon.awssdk.services.appconfigdata.model.InvalidParameterProblem;
import software.amazon.awssdk.services.appconfigdata.model.ResourceNotFoundException;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionRequest;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionResponse;
import software.amazon.awssdk.services.appconfigdata.model.ThrottlingException;

class AppConfigDataProviderTest {
    private static final Profile PROFILE = new Profile("app", "env", "profile", null);

    @Test
    void profileNormalizesAndHasUnambiguousRemoteIdentity() {
        assertEquals(PROFILE, new Profile(" app ", " env ", " profile ", null));
        assertEquals(PROFILE.id(), new Profile("app", "env", "profile", 30).id());
        assertNotEquals(new Profile("a:b", "c", "d", null).id(), new Profile("a", "b:c", "d", null).id());
        assertThrows(IllegalArgumentException.class, () -> new Profile(null, "env", "profile", null));
        assertThrows(IllegalArgumentException.class, () -> new Profile("app", " ", "profile", null));
        assertThrows(IllegalArgumentException.class, () -> new Profile("app", "env", "", null));
    }

    @ParameterizedTest
    @ValueSource(ints = { -1, 0, 14, 86401 })
    void rejectsInvalidSessionMinimum(int minimum) {
        assertThrows(IllegalArgumentException.class, () -> new Profile("app", "env", "profile", minimum));
    }

    @ParameterizedTest
    @ValueSource(ints = { 15, 86400 })
    void acceptsSessionMinimumBoundaries(int minimum) {
        StubClient client = new StubClient();
        try (var provider = new AppConfigDataProvider(client,
                List.of(new Profile("app", "env", "profile", minimum)))) {
            assertEquals(Duration.ofSeconds(minimum), provider.fetchAll().minDelayBeforeNextFetch());
            assertEquals(minimum, client.starts.getFirst().requiredMinimumPollIntervalInSeconds());
        }
    }

    @Test
    void preservesRawBytesAndMetadataAndAdvancesTokenWithoutDecoding() {
        StubClient client = new StubClient();
        byte[] invalidUtf8 = { (byte) 0xff, (byte) 0xfe };
        client.latest = request -> response("next-" + client.polls.size()).toBuilder()
                .configuration(SdkBytes.fromByteArray(invalidUtf8)).contentType("application/json").versionLabel("v1")
                .build();
        try (var provider = new AppConfigDataProvider(client, List.of(PROFILE))) {
            var first = provider.fetchAll();
            var document = first.updated().get(PROFILE.id());
            assertArrayEquals(invalidUtf8, document.content());
            assertEquals("application/json", document.contentType());
            assertEquals("v1", document.version());
            assertTrue(first.failed().isEmpty());
            assertNull(first.batchFailure());
            assertEquals(Duration.ofSeconds(60), first.minDelayBeforeNextFetch());
            provider.fetchAll();
            assertEquals(List.of("initial-1", "next-1"), client.tokens());
            assertEquals(1, client.starts.size());
            assertEquals(60, client.starts.getFirst().requiredMinimumPollIntervalInSeconds());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void initialEmptyBodyIsADocumentAndLaterEmptyBodyIsUnchanged(boolean nullBody) {
        StubClient client = new StubClient();
        client.latest = request -> response("next-" + client.polls.size()).toBuilder()
                .configuration(nullBody ? null : SdkBytes.fromByteArray(new byte[0])).build();
        try (var provider = new AppConfigDataProvider(client, List.of(PROFILE))) {
            var first = provider.fetchAll();
            assertArrayEquals(new byte[0], first.updated().get(PROFILE.id()).content());
            assertTrue(first.unchanged().isEmpty());
            var next = provider.fetchAll();
            assertTrue(next.updated().isEmpty());
            assertEquals(Set.of(PROFILE.id()), next.unchanged());
            assertEquals(List.of("initial-1", "next-1"), client.tokens());
        }
    }

    @Test
    void deduplicatesRemoteProfilesAndUsesTheirStrictestMinimum() {
        StubClient client = new StubClient();
        Profile other = new Profile("app", "env", "other", 30);
        client.latest = request -> response("next").toBuilder().nextPollIntervalInSeconds(75).build();
        try (var provider = new AppConfigDataProvider(client,
                List.of(PROFILE, new Profile("app", "env", "profile", 120), PROFILE, other))) {
            var result = provider.fetchAll();
            assertEquals(Set.of(PROFILE.id(), other.id()), result.updated().keySet());
            assertEquals(2, client.starts.size());
            assertEquals(2, client.polls.size());
            assertEquals(120, client.starts.get(0).requiredMinimumPollIntervalInSeconds());
            assertEquals(30, client.starts.get(1).requiredMinimumPollIntervalInSeconds());
            assertEquals(Duration.ofSeconds(120), result.minDelayBeforeNextFetch());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { -10, 0, 1, 90, 180 })
    void usesServerDelayWithoutAllowingBogusIntervalsToRemoveMinimum(int serverDelay) {
        StubClient client = new StubClient();
        client.latest = request -> response("next").toBuilder().nextPollIntervalInSeconds(serverDelay).build();
        try (var provider = new AppConfigDataProvider(client, List.of(PROFILE))) {
            assertEquals(Duration.ofSeconds(Math.max(60, serverDelay)), provider.fetchAll().minDelayBeforeNextFetch());
        }
    }

    @Test
    void retainsLastServerDelayOnFailureOrMissingInterval() {
        StubClient client = new StubClient();
        client.latest = request -> switch (client.polls.size()) {
            case 1 -> response("next").toBuilder().nextPollIntervalInSeconds(180).build();
            case 2 -> throw SdkClientException.create("lost response containing a sensitive token", new java.io.IOException());
            default -> response("next");
        };
        try (var provider = new AppConfigDataProvider(client, List.of(PROFILE))) {
            assertEquals(Duration.ofSeconds(180), provider.fetchAll().minDelayBeforeNextFetch());
            var failure = provider.fetchAll();
            assertEquals(Map.of(PROFILE.id(), FetchFailure.TRANSIENT), failure.failed());
            assertEquals(Duration.ofSeconds(180), failure.minDelayBeforeNextFetch());
            assertEquals(Duration.ofSeconds(180), provider.fetchAll().minDelayBeforeNextFetch());
            assertEquals(2, client.starts.size());
            assertEquals(List.of("initial-1", "next", "initial-2"), client.tokens());
        }
    }

    @ParameterizedTest
    @EnumSource(value = InvalidParameterProblem.class, names = { "EXPIRED", "CORRUPTED" })
    void rejectedTokenReopensOnlyOnNextFetch(InvalidParameterProblem problem) {
        StubClient client = new StubClient();
        client.latest = request -> {
            if (client.polls.size() == 1) {
                throw badRequest("ConfigurationToken", problem);
            }
            return response("next");
        };
        try (var provider = new AppConfigDataProvider(client, List.of(PROFILE))) {
            var failed = provider.fetchAll();
            assertEquals(Map.of(PROFILE.id(), FetchFailure.OTHER), failed.failed());
            assertEquals(Duration.ofSeconds(60), failed.minDelayBeforeNextFetch());
            assertEquals(1, client.starts.size());
            assertEquals(1, client.polls.size());
            assertEquals(Set.of(PROFILE.id()), provider.fetchAll().updated().keySet());
            assertEquals(2, client.starts.size());
            assertEquals(List.of("initial-1", "initial-2"), client.tokens());
        }
    }

    @Test
    void reopeningFailureDoesNotReplayTokenOrRetrySynchronously() {
        StubClient client = new StubClient();
        client.start = request -> {
            if (client.starts.size() == 2) {
                throw SdkClientException.create("start failed", new java.io.IOException());
            }
            return StartConfigurationSessionResponse.builder().initialConfigurationToken("initial-" + client.starts.size())
                    .build();
        };
        client.latest = request -> {
            if (client.polls.size() == 1) {
                throw SdkClientException.create("lost response", new java.io.IOException());
            }
            return response("next");
        };
        try (var provider = new AppConfigDataProvider(client, List.of(PROFILE))) {
            assertEquals(FetchFailure.TRANSIENT, provider.fetchAll().failed().get(PROFILE.id()));
            var reopening = provider.fetchAll();
            assertEquals(FetchFailure.TRANSIENT, reopening.failed().get(PROFILE.id()));
            assertEquals(Duration.ofSeconds(60), reopening.minDelayBeforeNextFetch());
            assertEquals(2, client.starts.size());
            assertEquals(1, client.polls.size());
            assertEquals(Set.of(PROFILE.id()), provider.fetchAll().updated().keySet());
            assertEquals(List.of("initial-1", "initial-3"), client.tokens());
        }
    }

    @Test
    void nonTokenBadRequestIsAConfigurationFailureWithoutReopening() {
        StubClient client = new StubClient();
        client.latest = request -> {
            throw badRequest("ApplicationIdentifier", InvalidParameterProblem.CORRUPTED);
        };
        try (var provider = new AppConfigDataProvider(client, List.of(PROFILE))) {
            assertEquals(Map.of(PROFILE.id(), FetchFailure.OTHER), provider.fetchAll().failed());
            assertEquals(Map.of(PROFILE.id(), FetchFailure.OTHER), provider.fetchAll().failed());
            assertEquals(1, client.starts.size());
            assertEquals(List.of("initial-1", "initial-1"), client.tokens());
        }
    }

    @Test
    void pollIntervalErrorDoesNotDiscardAStillValidToken() {
        StubClient client = new StubClient();
        client.latest = request -> {
            if (client.polls.size() == 1) {
                throw badRequest("ConfigurationToken", InvalidParameterProblem.POLL_INTERVAL_NOT_SATISFIED);
            }
            return response("next");
        };
        try (var provider = new AppConfigDataProvider(client, List.of(PROFILE))) {
            assertEquals(Map.of(PROFILE.id(), FetchFailure.OTHER), provider.fetchAll().failed());
            assertEquals(Set.of(PROFILE.id()), provider.fetchAll().updated().keySet());
            assertEquals(1, client.starts.size());
            assertEquals(List.of("initial-1", "initial-1"), client.tokens());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 403, 404, 429, 500 })
    void isolatesProfileFailureAndResetsUncertainSession(int status) {
        StubClient client = new StubClient();
        Profile other = new Profile("app", "env", "other", 120);
        client.latest = request -> {
            if (request.configurationToken().equals("initial-1")) {
                throw AppConfigDataException.builder().statusCode(status).message("sensitive service message").build();
            }
            return response("next");
        };
        try (var provider = new AppConfigDataProvider(client, List.of(PROFILE, other))) {
            var result = provider.fetchAll();
            assertEquals(Map.of(PROFILE.id(), status == 403 ? FetchFailure.ACCESS_DENIED
                    : status == 404 ? FetchFailure.MISSING : FetchFailure.TRANSIENT), result.failed());
            assertEquals(Set.of(other.id()), result.updated().keySet());
            assertTrue(result.removed().isEmpty());
            assertEquals(Duration.ofSeconds(120), result.minDelayBeforeNextFetch());
            provider.fetchAll();
            assertEquals(3, client.starts.size());
            assertEquals(List.of("initial-1", "initial-2", "initial-3", "next"), client.tokens());
        }
    }

    @Test
    void missingInitialTokenReportsPartialResponseAndTriesSessionNextTime() {
        StubClient client = new StubClient();
        client.start = request -> StartConfigurationSessionResponse.builder().build();
        try (var provider = new AppConfigDataProvider(client, List.of(PROFILE))) {
            var first = provider.fetchAll();
            assertEquals(Map.of(PROFILE.id(), FetchFailure.PARTIAL_RESPONSE), first.failed());
            assertEquals(Duration.ofSeconds(60), first.minDelayBeforeNextFetch());
            provider.fetchAll();
            assertEquals(2, client.starts.size());
            assertTrue(client.polls.isEmpty());
        }
    }

    @Test
    void classifiesModeledFailuresWithoutHttpMetadata() {
        StubClient client = new StubClient();
        client.start = request -> {
            if (client.starts.size() == 1) {
                throw ResourceNotFoundException.builder().build();
            }
            throw ThrottlingException.builder().build();
        };
        try (var provider = new AppConfigDataProvider(client, List.of(PROFILE))) {
            assertEquals(Map.of(PROFILE.id(), FetchFailure.MISSING), provider.fetchAll().failed());
            assertEquals(Map.of(PROFILE.id(), FetchFailure.TRANSIENT), provider.fetchAll().failed());
            assertTrue(client.polls.isEmpty());
        }
    }

    @Test
    void missingNextTokenNeverReplaysConsumedToken() {
        StubClient client = new StubClient();
        client.latest = request -> response(null);
        try (var provider = new AppConfigDataProvider(client, List.of(PROFILE))) {
            assertFalse(provider.fetchAll().updated().isEmpty());
            provider.fetchAll();
            assertEquals(List.of("initial-1", "initial-2"), client.tokens());
        }
    }

    @Test
    void ownsClientAndClosesOnlyOnce() {
        StubClient client = new StubClient();
        var provider = new AppConfigDataProvider(client, List.of(PROFILE));
        assertTrue(client.starts.isEmpty());
        provider.close();
        provider.close();
        assertEquals(1, client.closes);
        assertThrows(IllegalStateException.class, provider::fetchAll);
        assertTrue(client.polls.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void closingAnActiveFetchPreventsSubsequentRequests(boolean duringSessionStart) throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        Runnable block = () -> {
            entered.countDown();
            try {
                assertTrue(release.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        };
        var client = new StubClient();
        client.start = request -> {
            if (duringSessionStart) {
                block.run();
            }
            return StartConfigurationSessionResponse.builder().initialConfigurationToken("initial").build();
        };
        client.latest = request -> {
            if (!duringSessionStart) {
                block.run();
            }
            return response("next");
        };
        var executor = Executors.newSingleThreadExecutor();
        try (var provider = new AppConfigDataProvider(client, List.of(PROFILE, new Profile("app", "env", "other", null)))) {
            var fetch = executor.submit(provider::fetchAll);
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            provider.close();
            release.countDown();
            var error = assertThrows(ExecutionException.class, () -> fetch.get(5, TimeUnit.SECONDS));
            assertEquals(IllegalStateException.class, error.getCause().getClass());
            assertEquals(1, client.starts.size());
            assertEquals(duringSessionStart ? 0 : 1, client.polls.size());
            assertEquals(1, client.closes);
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @ParameterizedTest
    @CsvSource({ "-1,false", "429,false", "500,false", "-1,true", "429,true", "500,true" })
    @SuppressWarnings("deprecation")
    void realSdkDoesNotRetryPollEvenWithCallerSuppliedRetryingClient(int failureStatus, boolean legacyPolicy) {
        PollHttpClient http = new PollHttpClient(failureStatus);
        AppConfigDataClient client = AppConfigDataClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .endpointOverride(URI.create("http://localhost"))
                .httpClient(http)
                .overrideConfiguration(overrides -> {
                    if (legacyPolicy) {
                        overrides.retryPolicy(RetryPolicy.builder().numRetries(3).build());
                    } else {
                        overrides.retryStrategy(StandardRetryStrategy.builder().maxAttempts(4).build());
                    }
                })
                .build();
        try (var provider = new AppConfigDataProvider(client, List.of(PROFILE))) {
            assertEquals(Map.of(PROFILE.id(), FetchFailure.TRANSIENT), provider.fetchAll().failed());
            assertEquals(1, http.polls);
            assertEquals(1, http.starts);
            assertEquals(Set.of(PROFILE.id()), provider.fetchAll().updated().keySet());
            assertEquals(2, http.polls);
            assertEquals(2, http.starts);

            // The request plugin must not disable retries on the supplied client itself.
            http.failNext = true;
            client.getLatestConfiguration(
                    GetLatestConfigurationRequest.builder().configurationToken("outside-provider").build());
            assertEquals(4, http.polls);
        }
    }

    private static GetLatestConfigurationResponse response(String nextToken) {
        return GetLatestConfigurationResponse.builder().nextPollConfigurationToken(nextToken)
                .configuration(SdkBytes.fromUtf8String("{}"))
                .build();
    }

    private static BadRequestException badRequest(String parameter, InvalidParameterProblem problem) {
        return BadRequestException.builder().statusCode(400).reason(BadRequestReason.INVALID_PARAMETERS)
                .details(BadRequestDetails.fromInvalidParameters(
                        Map.of(parameter, InvalidParameterDetail.builder().problem(problem).build())))
                .build();
    }

    private static final class StubClient implements AppConfigDataClient {
        final List<StartConfigurationSessionRequest> starts = new ArrayList<>();
        final List<GetLatestConfigurationRequest> polls = new ArrayList<>();
        Function<StartConfigurationSessionRequest, StartConfigurationSessionResponse> start = request -> StartConfigurationSessionResponse
                .builder().initialConfigurationToken("initial-" + starts.size()).build();
        Function<GetLatestConfigurationRequest, GetLatestConfigurationResponse> latest = request -> response("next");
        int closes;

        List<String> tokens() {
            return polls.stream().map(GetLatestConfigurationRequest::configurationToken).toList();
        }

        @Override
        public StartConfigurationSessionResponse startConfigurationSession(StartConfigurationSessionRequest request) {
            starts.add(request);
            return start.apply(request);
        }

        @Override
        public GetLatestConfigurationResponse getLatestConfiguration(GetLatestConfigurationRequest request) {
            polls.add(request);
            return latest.apply(request);
        }

        @Override
        public String serviceName() {
            return "appconfigdata";
        }

        @Override
        public void close() {
            closes++;
        }
    }

    private static final class PollHttpClient implements SdkHttpClient {
        final int failureStatus;
        boolean failNext = true;
        int starts;
        int polls;

        PollHttpClient(int failureStatus) {
            this.failureStatus = failureStatus;
        }

        @Override
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
            return new ExecutableHttpRequest() {
                @Override
                public HttpExecuteResponse call() throws IOException {
                    if (request.httpRequest().method() == SdkHttpMethod.POST) {
                        starts++;
                        return httpResponse(200, "{\"InitialConfigurationToken\":\"initial-" + starts + "\"}", Map.of());
                    }
                    polls++;
                    if (failNext) {
                        failNext = false;
                        if (failureStatus == -1) {
                            throw new IOException("response lost after consuming token");
                        }
                        return httpResponse(failureStatus, "{}", Map.of("x-amzn-ErrorType",
                                failureStatus == 429 ? "ThrottlingException" : "InternalServerException"));
                    }
                    return httpResponse(200, "{}", Map.of("Next-Poll-Configuration-Token", "next-" + polls,
                            "Next-Poll-Interval-In-Seconds", "60"));
                }

                @Override
                public void abort() {
                }
            };
        }

        @Override
        public void close() {
        }

        private static HttpExecuteResponse httpResponse(int status, String body, Map<String, String> headers) {
            var response = SdkHttpResponse.builder().statusCode(status).putHeader("Content-Type", "application/json");
            headers.forEach(response::putHeader);
            return HttpExecuteResponse.builder().response(response.build())
                    .responseBody(AbortableInputStream.create(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))))
                    .build();
        }
    }
}
