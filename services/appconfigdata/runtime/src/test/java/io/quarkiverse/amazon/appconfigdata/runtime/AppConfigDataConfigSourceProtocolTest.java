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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.amazon.appconfigdata.runtime.AppConfigDataProvider.Profile;
import io.quarkiverse.amazon.common.runtime.ConfigSourceFormat;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationRequest;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationResponse;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionRequest;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionResponse;

class AppConfigDataConfigSourceProtocolTest {
    @ParameterizedTest
    @ValueSource(strings = { "1H", "PT1H", "3600" })
    void bootstrapMappingAcceptsQuarkusPollingDurations(String value) {
        var config = new SmallRyeConfigBuilder().withMapping(AppConfigDataConfigConfig.class)
                .withDefaultValue("quarkus.appconfigdata.config.update-interval", value).build();
        assertEquals(Duration.ofHours(1), config.getConfigMapping(AppConfigDataConfigConfig.class).updateInterval());
    }

    @Test
    void legacyAndNamedProfilesShareSessionsButKeepPrefixesFormatsAndMinima() {
        var mapping = new SmallRyeConfigBuilder().withMapping(AppConfigDataConfigConfig.class)
                .withDefaultValue("quarkus.appconfigdata.config.enabled", "true")
                .withDefaultValue("quarkus.appconfigdata.config.application", "app")
                .withDefaultValue("quarkus.appconfigdata.config.environment", "env")
                .withDefaultValue("quarkus.appconfigdata.config.configuration-profile", "A")
                .withDefaultValue("quarkus.appconfigdata.config.required-minimum-poll-interval-in-seconds", "15")
                .withSources(new MapBackedConfigSource("test", Map.ofEntries(
                        Map.entry("quarkus.appconfigdata.config.entries.copy.application", "app"),
                        Map.entry("quarkus.appconfigdata.config.entries.copy.environment", "env"),
                        Map.entry("quarkus.appconfigdata.config.entries.copy.configuration-profile", "A"),
                        Map.entry("quarkus.appconfigdata.config.entries.copy.prefix", "opaque"),
                        Map.entry("quarkus.appconfigdata.config.entries.copy.format", "raw"),
                        Map.entry("quarkus.appconfigdata.config.entries.copy.required-minimum-poll-interval-in-seconds", "30"),
                        Map.entry("quarkus.appconfigdata.config.entries.other.application", "app"),
                        Map.entry("quarkus.appconfigdata.config.entries.other.environment", "env"),
                        Map.entry("quarkus.appconfigdata.config.entries.other.configuration-profile", "B"),
                        Map.entry("quarkus.appconfigdata.config.entries.other.prefix", "settings"),
                        Map.entry("quarkus.appconfigdata.config.entries.other.format", "yaml"))) {
                })
                .build().getConfigMapping(AppConfigDataConfigConfig.class);
        var client = new ProtocolClient();
        var minima = new HashMap<String, Integer>();
        client.start = request -> {
            minima.put(request.configurationProfileIdentifier(), request.requiredMinimumPollIntervalInSeconds());
            return StartConfigurationSessionResponse.builder()
                    .initialConfigurationToken(request.configurationProfileIdentifier()).build();
        };
        client.latest = request -> response(request.configurationToken().equals("A") ? "{\"enabled\":true}" : "b: 1", "next");
        var factory = new AppConfigDataConfigSourceFactory() {
            AppConfigDataConfig getClientConfig(ConfigSourceContext context) {
                return null;
            }

            AppConfigDataClient build(AppConfigDataConfig config) {
                return client;
            }
        };
        var source = factory.getConfigSources(null, mapping).iterator().next();
        assertEquals("true", source.getValue("enabled"));
        assertEquals("{\"enabled\":true}", source.getValue("opaque"));
        assertEquals("1", source.getValue("settings.b"));
        assertEquals(Map.of("A", 30, "B", 15), minima);
        assertEquals(2, client.starts);
        assertEquals(2, client.used.size());
        assertEquals(1, client.closes);
    }

    @Test
    void namedProfileDoesNotRequireLegacyIdentifiers() {
        var mapping = new SmallRyeConfigBuilder().withMapping(AppConfigDataConfigConfig.class)
                .withDefaultValue("quarkus.appconfigdata.config.entries.main.application", "app")
                .withDefaultValue("quarkus.appconfigdata.config.entries.main.environment", "env")
                .withDefaultValue("quarkus.appconfigdata.config.entries.main.configuration-profile", "A")
                .build().getConfigMapping(AppConfigDataConfigConfig.class);
        var selections = AppConfigDataConfigSourceFactory.selections(mapping);
        assertEquals(1, selections.size());
        assertEquals("A", selections.getFirst().profile().configurationProfile());
        assertEquals("", selections.getFirst().prefix());
        assertEquals(ConfigSourceFormat.AUTO, selections.getFirst().format());
    }

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
        assertThrows(IllegalArgumentException.class,
                () -> new AppConfigDataConfigSourceFactory().getConfigSources(null, missing));
        var optional = new SmallRyeConfigBuilder().withMapping(AppConfigDataConfigConfig.class)
                .withDefaultValue("quarkus.appconfigdata.config.enabled", "true")
                .withDefaultValue("quarkus.appconfigdata.config.fail-on-missing-config", "false")
                .build().getConfigMapping(AppConfigDataConfigConfig.class);
        assertFalse(new AppConfigDataConfigSourceFactory().getConfigSources(null, optional).iterator().hasNext());
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

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void factoryAppliesStartupPolicyAndOrdinal(boolean required) {
        var mapping = new SmallRyeConfigBuilder().withMapping(AppConfigDataConfigConfig.class)
                .withDefaultValue("quarkus.appconfigdata.config.enabled", "true")
                .withDefaultValue("quarkus.appconfigdata.config.application", "app")
                .withDefaultValue("quarkus.appconfigdata.config.environment", "env")
                .withDefaultValue("quarkus.appconfigdata.config.configuration-profile", "profile")
                .withDefaultValue("quarkus.appconfigdata.config.ordinal", "301")
                .withDefaultValue("quarkus.appconfigdata.config.fail-on-missing-config", Boolean.toString(required))
                .build().getConfigMapping(AppConfigDataConfigConfig.class);
        var client = new ProtocolClient();
        client.start = request -> {
            throw software.amazon.awssdk.services.appconfigdata.model.ResourceNotFoundException.builder()
                    .message("unsafe token").build();
        };
        var factory = new AppConfigDataConfigSourceFactory() {
            AppConfigDataConfig getClientConfig(ConfigSourceContext context) {
                return null;
            }

            AppConfigDataClient build(AppConfigDataConfig config) {
                return client;
            }
        };
        if (required) {
            var failure = assertThrows(IllegalStateException.class, () -> factory.getConfigSources(null, mapping));
            assertEquals("Unable to load quarkus.appconfigdata.config [MISSING]", failure.getMessage());
        } else {
            var source = factory.getConfigSources(null, mapping).iterator().next();
            assertEquals(301, source.getOrdinal());
            assertTrue(source.getPropertyNames().isEmpty());
        }
        assertEquals(1, client.closes);
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
