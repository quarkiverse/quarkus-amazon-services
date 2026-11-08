package io.quarkiverse.amazon.appconfigdata.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import io.quarkiverse.amazon.appconfigdata.runtime.AppConfigDataProvider.Profile;
import io.quarkiverse.amazon.common.runtime.ConfigSourceFormat;
import io.smallrye.config.SmallRyeConfigBuilder;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationRequest;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationResponse;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionRequest;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionResponse;

class AppConfigDataConfigSourceProtocolTest {
    @Test
    void partialCycleRetainsChangedProfileWhenItsNextResponseIsUnchanged() {
        var client = new ProtocolClient();
        Map<String, Integer> polls = new HashMap<>();
        client.latest = request -> {
            String profile = request.configurationToken().substring(0, 1);
            int poll = polls.merge(profile, 1, Integer::sum);
            if (profile.equals("B") && poll == 2) {
                throw SdkClientException.create("response lost after consuming token");
            }
            String body = profile.equals("A") ? switch (poll) {
                case 1 -> "{\"a\":\"old\",\"gone\":true}";
                case 2 -> "{\"a\":\"new\"}";
                default -> "";
            } : poll == 1 ? "{\"b\":\"old\"}" : "{\"b\":\"new\"}";
            return response(body, profile + "-next-" + poll);
        };
        try (var source = new AppConfigDataConfigSource(client, List.of(profile("A"), profile("B")), Duration.ofHours(1),
                270, ConfigSourceFormat.AUTO)) {
            assertEquals("old", source.getValue("a"));
            source.fetchConfig();
            assertEquals("old", source.getValue("a"));
            assertEquals("old", source.getValue("b"));
            assertEquals("true", source.getValue("gone"));
            source.fetchConfig();
            assertEquals("new", source.getValue("a"));
            assertEquals("new", source.getValue("b"));
            assertNull(source.getValue("gone"));
            assertEquals(3, client.starts);
            assertEquals(6, client.used.size());
        }
        assertEquals(1, client.closes);
    }

    @Test
    void lostResponseAndFailedReopeningPreserveSnapshotUntilANewSessionSucceeds() {
        var client = new ProtocolClient();
        client.start = request -> {
            if (client.starts == 2) {
                throw SdkClientException.create("session unavailable");
            }
            return StartConfigurationSessionResponse.builder().initialConfigurationToken("A-initial-" + client.starts).build();
        };
        client.latest = request -> {
            if (client.used.size() == 2) {
                throw SdkClientException.create("response lost after consuming token");
            }
            return response(client.used.size() == 1 ? "{\"a\":\"old\"}" : "{\"a\":\"new\"}", "A-next-" + client.used.size());
        };
        try (var source = new AppConfigDataConfigSource(client, List.of(profile("A")), Duration.ofHours(1), 270,
                ConfigSourceFormat.AUTO)) {
            source.fetchConfig();
            assertEquals("old", source.getValue("a"));
            source.fetchConfig();
            assertEquals("old", source.getValue("a"));
            assertEquals(2, client.used.size());
            source.fetchConfig();
            assertEquals("new", source.getValue("a"));
            assertEquals(List.of("A-initial-1", "A-next-1", "A-initial-3"), client.tokens);
        }
    }

    @Test
    void factoryRejectsInvalidMinimumBeforeCreatingAClientAndSkipsMissingIdentifiers() {
        var builder = new SmallRyeConfigBuilder().withMapping(AppConfigDataConfigConfig.class)
                .withDefaultValue("quarkus.appconfigdata.config.enabled", "true");
        var missing = builder.build().getConfigMapping(AppConfigDataConfigConfig.class);
        assertFalse(new AppConfigDataConfigSourceFactory().getConfigSources(null, missing).iterator().hasNext());
        var invalid = new SmallRyeConfigBuilder().withMapping(AppConfigDataConfigConfig.class)
                .withDefaultValue("quarkus.appconfigdata.config.enabled", "true")
                .withDefaultValue("quarkus.appconfigdata.config.application", "app")
                .withDefaultValue("quarkus.appconfigdata.config.environment", "env")
                .withDefaultValue("quarkus.appconfigdata.config.configuration-profile", "profile")
                .withDefaultValue("quarkus.appconfigdata.config.required-minimum-poll-interval-in-seconds", "0")
                .build().getConfigMapping(AppConfigDataConfigConfig.class);
        var error = assertThrows(IllegalArgumentException.class,
                () -> new AppConfigDataConfigSourceFactory().getConfigSources(null, invalid));
        assertTrue(error.getMessage().contains("between 15 and 86400"));
    }

    private Profile profile(String name) {
        return new Profile("app", "env", name, null);
    }

    private static GetLatestConfigurationResponse response(String body, String token) {
        return GetLatestConfigurationResponse.builder().configuration(SdkBytes.fromUtf8String(body))
                .nextPollConfigurationToken(token).nextPollIntervalInSeconds(60).contentType("application/json").build();
    }

    private static final class ProtocolClient implements AppConfigDataClient {
        final Set<String> used = new HashSet<>();
        final List<String> tokens = new ArrayList<>();
        int starts;
        int closes;
        Function<StartConfigurationSessionRequest, StartConfigurationSessionResponse> start = request -> StartConfigurationSessionResponse
                .builder().initialConfigurationToken(request.configurationProfileIdentifier() + "-initial-" + starts).build();
        Function<GetLatestConfigurationRequest, GetLatestConfigurationResponse> latest;

        public StartConfigurationSessionResponse startConfigurationSession(StartConfigurationSessionRequest request) {
            starts++;
            return start.apply(request);
        }

        public GetLatestConfigurationResponse getLatestConfiguration(GetLatestConfigurationRequest request) {
            assertTrue(used.add(request.configurationToken()), "A consumed token was replayed");
            tokens.add(request.configurationToken());
            return latest.apply(request);
        }

        public String serviceName() {
            return "appconfigdata";
        }

        public void close() {
            closes++;
        }
    }
}
