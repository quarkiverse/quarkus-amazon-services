package io.quarkiverse.amazon.appconfigdata.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationRequest;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationResponse;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionRequest;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionResponse;

class AppConfigDataConfigSourceTest {

    @Test
    void parseJsonFlattensNestedObjectsAndArrays() {
        Map<String, String> map = AppConfigDataConfigSource.parseConfiguration("""
                {
                  "app": {"name": "demo", "port": 8080, "enabled": true},
                  "hosts": ["a", {"url": "b"}]
                }
                """);

        assertEquals(Map.of(
                "app.name", "demo",
                "app.port", "8080",
                "app.enabled", "true",
                "hosts[0]", "a",
                "hosts[1].url", "b"), map);
    }

    @Test
    void parsePropertiesText() {
        Map<String, String> map = AppConfigDataConfigSource.parseConfiguration("""
                # comment
                greeting.message=hello
                feature.enabled = true
                """);

        assertEquals(Map.of("greeting.message", "hello", "feature.enabled", "true"), map);
    }

    @Test
    void parseInvalidJsonReturnsEmpty() {
        assertTrue(AppConfigDataConfigSource.parseConfiguration("{\"a\": ").isEmpty());
    }

    @Test
    void parseEmptyOrBlankReturnsEmpty() {
        assertTrue(AppConfigDataConfigSource.parseConfiguration("").isEmpty());
        assertTrue(AppConfigDataConfigSource.parseConfiguration("  \n\t ").isEmpty());
    }

    @Test
    void constructorLoadsConfigurationFromClient() {
        StubClient client = new StubClient(call -> json("{\"greeting\": {\"message\": \"hello\"}}", "next-token"));

        try (var source = new AppConfigDataConfigSource(client, " app ", "env", "profile", 30, Duration.ZERO, 275)) {
            assertEquals("hello", source.getValue("greeting.message"));
            assertEquals(Set.of("greeting.message"), source.getPropertyNames());
            assertEquals(275, source.getOrdinal());

            // identifiers are trimmed, optional poll interval is forwarded, initial token is used for the first fetch
            assertEquals("app", client.sessionRequest.get().applicationIdentifier());
            assertEquals(30, client.sessionRequest.get().requiredMinimumPollIntervalInSeconds());
            assertEquals("initial-token", client.latestRequest.get().configurationToken());
        }
        assertTrue(client.closed.get());
    }

    @Test
    void constructorWithNullConfigurationExposesNoEntries() {
        StubClient client = new StubClient(
                call -> GetLatestConfigurationResponse.builder().nextPollConfigurationToken("next-token").build());

        try (var source = new AppConfigDataConfigSource(client, "app", "env", "profile", null, Duration.ZERO, 275)) {
            assertTrue(source.getPropertyNames().isEmpty());
            assertNull(source.getValue("anything"));
        }
    }

    @Test
    void missingIdentifiersSkipSession() {
        StubClient client = new StubClient(call -> null);

        try (var source = new AppConfigDataConfigSource(client, "app", " ", "profile", null, Duration.ZERO, 275)) {
            assertTrue(source.getPropertyNames().isEmpty());
            assertNull(client.sessionRequest.get());
        }
    }

    @Test
    void fetchFailureIsNotFatalAndNextFetchRecovers() {
        StubClient client = new StubClient(call -> {
            if (call == 1) {
                throw SdkClientException.create("network error");
            }
            return json("{\"a\":1}", "t1");
        });

        // a poll interval keeps the client open; the poller does not fire during the test
        try (var source = new AppConfigDataConfigSource(client, "app", "env", "profile", null, Duration.ofMinutes(60), 275)) {
            assertNull(source.getValue("a"));

            source.fetchConfig();
            assertEquals("1", source.getValue("a"));
            assertEquals(2, client.calls.get());
        }
    }

    @Test
    void unchangedPayloadKeepsPreviousEntriesAndAdvancesToken() {
        StubClient client = new StubClient(call -> call == 1
                ? json("{\"a\":1}", "t1")
                : GetLatestConfigurationResponse.builder().nextPollConfigurationToken("t2").build());

        try (var source = new AppConfigDataConfigSource(client, "app", "env", "profile", null, Duration.ofMinutes(60), 275)) {
            source.fetchConfig();

            assertEquals("1", source.getValue("a"));
            assertEquals("t1", client.latestRequest.get().configurationToken());
        }
    }

    @Test
    void withoutPollingClientIsClosedAfterInitialLoad() {
        StubClient client = new StubClient(call -> json("{\"a\":1}", "t1"));

        var source = new AppConfigDataConfigSource(client, "app", "env", "profile", null, Duration.ZERO, 275);

        assertTrue(client.closed.get());
        assertEquals("1", source.getValue("a"));
    }

    @Test
    void closeStopsPolling() {
        StubClient client = new StubClient(call -> json("{\"a\":" + call + "}", "t" + call));

        var source = new AppConfigDataConfigSource(client, "app", "env", "profile", null, Duration.ofMinutes(60), 275);
        source.close();
        source.fetchConfig();

        assertTrue(client.closed.get());
        assertEquals(1, client.calls.get());
        assertEquals("1", source.getValue("a"));
    }

    private static GetLatestConfigurationResponse json(String content, String nextToken) {
        return GetLatestConfigurationResponse.builder()
                .configuration(SdkBytes.fromUtf8String(content))
                .nextPollConfigurationToken(nextToken)
                .build();
    }

    private static final class StubClient implements AppConfigDataClient {
        final IntFunction<GetLatestConfigurationResponse> responses;
        final AtomicInteger calls = new AtomicInteger();
        final AtomicReference<StartConfigurationSessionRequest> sessionRequest = new AtomicReference<>();
        final AtomicReference<GetLatestConfigurationRequest> latestRequest = new AtomicReference<>();
        final AtomicBoolean closed = new AtomicBoolean();

        StubClient(IntFunction<GetLatestConfigurationResponse> responses) {
            this.responses = responses;
        }

        @Override
        public StartConfigurationSessionResponse startConfigurationSession(StartConfigurationSessionRequest request) {
            sessionRequest.set(request);
            return StartConfigurationSessionResponse.builder().initialConfigurationToken("initial-token").build();
        }

        @Override
        public GetLatestConfigurationResponse getLatestConfiguration(GetLatestConfigurationRequest request) {
            latestRequest.set(request);
            return responses.apply(calls.incrementAndGet());
        }

        @Override
        public String serviceName() {
            return "appconfigdata";
        }

        @Override
        public void close() {
            closed.set(true);
        }
    }
}
