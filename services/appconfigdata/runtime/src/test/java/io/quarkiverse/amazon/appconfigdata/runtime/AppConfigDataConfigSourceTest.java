package io.quarkiverse.amazon.appconfigdata.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.amazon.common.runtime.ConfigSourceFormat;
import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;
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

    @ParameterizedTest
    @ValueSource(strings = { "{\"password\":\"sensitive-value\",", "[\"sensitive-value\",", "password=\\uZZZZ" })
    void parseInvalidContentThrowsWithoutExposingValues(String content) {
        var error = assertThrows(IllegalArgumentException.class,
                () -> AppConfigDataConfigSource.parseConfiguration(content));

        assertTrue(Set
                .of("Invalid JSON configuration content.",
                        "Invalid Java properties configuration content.")
                .contains(error.getMessage()));
        assertNull(error.getCause());
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

    @ParameterizedTest
    @ValueSource(strings = { "{\"password\":\"sensitive-value\",", "[\"sensitive-value\",", "password=\\uZZZZ" })
    void invalidPayloadKeepsPreviousEntriesAndAdvancesToken(String content) {
        StubClient client = new StubClient(call -> switch (call) {
            case 1 -> json("{\"a\":1,\"b\":2}", "t1");
            case 2 -> json(content, "t2");
            case 3 -> json("", "t3");
            default -> json("{\"a\":3}", "t4");
        });

        try (var source = new AppConfigDataConfigSource(client, "app", "env", "profile", null, Duration.ofMinutes(60), 275)) {
            source.fetchConfig();

            assertEquals("t1", client.latestRequest.get().configurationToken());
            assertEquals(Set.of("a", "b"), source.getPropertyNames());
            assertEquals("1", source.getValue("a"));
            assertEquals("2", source.getValue("b"));

            source.fetchConfig();

            assertEquals("t2", client.latestRequest.get().configurationToken());
            assertEquals(Set.of("a", "b"), source.getPropertyNames());
            assertEquals("1", source.getValue("a"));
            assertEquals("2", source.getValue("b"));

            source.fetchConfig();

            assertEquals("t3", client.latestRequest.get().configurationToken());
            assertEquals(Set.of("a"), source.getPropertyNames());
            assertEquals("3", source.getValue("a"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "{}", "[]", "{\"a\":null}", "  \n\t " })
    void validEmptyDocumentClearsPreviousEntries(String content) {
        StubClient client = new StubClient(call -> call == 1 ? json("{\"a\":1}", "t1") : json(content, "t2"));

        try (var source = new AppConfigDataConfigSource(client, "app", "env", "profile", null, Duration.ofMinutes(60), 275)) {
            assertEquals("1", source.getValue("a"));

            source.fetchConfig();

            assertTrue(source.getPropertyNames().isEmpty());
            assertNull(source.getValue("a"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "null", "true", "123", "\"abc\"", "  \"abc\"  " })
    void autoScalarsKeepLegacyPropertiesParsing(String content) {
        assertEquals(ConfigSourceFormat.PROPERTIES.decode("", content), AppConfigDataConfigSource.parseConfiguration(content));
    }

    @Test
    void declaredPropertiesOverridesJsonDetection() {
        StubClient client = new StubClient(call -> json("{key=value", "next-token"));
        try (var source = new AppConfigDataConfigSource(client, "app", "env", "profile", null, Duration.ZERO, 270,
                ConfigSourceFormat.PROPERTIES)) {
            assertEquals("value", source.getValue("{key"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "{\"a\":1} trailing", "{\"a\":1} {}", "null false", "[1] [2]" })
    void explicitJsonWithTrailingContentKeepsPreviousSnapshot(String content) {
        StubClient client = new StubClient(call -> call == 1 ? json("{\"a\":1,\"b\":2}", "t1") : json(content, "t2"));
        try (var source = new AppConfigDataConfigSource(client, "app", "env", "profile", null, Duration.ofHours(1), 270,
                ConfigSourceFormat.JSON)) {
            source.fetchConfig();
            assertEquals(Set.of("a", "b"), source.getPropertyNames());
            assertEquals("1", source.getValue("a"));
            assertEquals("2", source.getValue("b"));
        }
    }

    @Test
    void unknownFormatFailsConfigurationMapping() {
        assertThrows(ConfigValidationException.class,
                () -> new SmallRyeConfigBuilder().withMapping(AppConfigDataConfigConfig.class)
                        .withSources(
                                new MapBackedConfigSource("test", Map.of("quarkus.appconfigdata.config.format", "invalid")) {
                                })
                        .build());
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

    @ParameterizedTest
    @ValueSource(strings = { "application/yaml", "application/x-yaml", "text/yaml", "text/x-yaml",
            "Application/YAML; charset=utf-8", "application/vnd.example+yaml" })
    void autoRecognizesYamlContentTypes(String mime) {
        assertEquals(Map.of("feature.enabled", "true"),
                AppConfigDataConfigSource.parseConfiguration("feature:\n  enabled: true\n", mime));
        assertEquals(Map.of("feature", "true"), AppConfigDataConfigSource.parseConfiguration("{feature: true}", mime));
    }

    @ParameterizedTest
    @ValueSource(strings = { "text/plain", "application/json", "application/yaml" })
    void autoJsonHasPriorityOverMime(String mime) {
        assertEquals(Map.of("feature.enabled", "true"),
                AppConfigDataConfigSource.parseConfiguration("{\"feature\":{\"enabled\":true}}", mime));
    }

    @ParameterizedTest
    @ValueSource(strings = { "password: [secret", "a: &a\n  b: *a\n" })
    void invalidYamlKeepsSnapshotAndAdvancesToken(String invalidContent) {
        StubClient client = new StubClient(call -> GetLatestConfigurationResponse.builder()
                .configuration(SdkBytes.fromUtf8String(switch (call) {
                    case 1 -> "feature:\n  enabled: true\n";
                    case 2 -> invalidContent;
                    default -> "feature: false";
                })).contentType("application/yaml").nextPollConfigurationToken("t" + call).build());
        try (var source = new AppConfigDataConfigSource(client, "app", "env", "profile", null, Duration.ofHours(1), 270)) {
            assertEquals("true", source.getValue("feature.enabled"));
            source.fetchConfig();
            assertEquals("true", source.getValue("feature.enabled"));
            source.fetchConfig();
            assertEquals("t2", client.latestRequest.get().configurationToken());
            assertEquals("false", source.getValue("feature"));
            assertNull(source.getValue("feature.enabled"));
        }
    }

    @Test
    void declaredFormatOverridesYamlMime() {
        StubClient client = new StubClient(call -> GetLatestConfigurationResponse.builder()
                .configuration(SdkBytes.fromUtf8String("feature:\n  enabled: true\n"))
                .contentType("application/yaml").nextPollConfigurationToken("t1").build());
        try (var source = new AppConfigDataConfigSource(client, "app", "env", "profile", null, Duration.ZERO, 270,
                ConfigSourceFormat.PROPERTIES)) {
            assertEquals("", source.getValue("feature"));
            assertEquals("true", source.getValue("enabled"));
        }
    }

    @Test
    void invalidUtf8KeepsSnapshotAndAdvancesToken() {
        StubClient client = new StubClient(call -> call == 2 ? GetLatestConfigurationResponse.builder()
                .configuration(SdkBytes.fromByteArray(new byte[] { (byte) 0xff, (byte) 0xfe }))
                .nextPollConfigurationToken("t2").build() : json("{\"a\":1}", "t" + call));
        try (var source = new AppConfigDataConfigSource(client, "app", "env", "profile", null, Duration.ofHours(1), 270)) {
            source.fetchConfig();
            assertEquals("1", source.getValue("a"));
            source.fetchConfig();
            assertEquals("t2", client.latestRequest.get().configurationToken());
            assertEquals("1", source.getValue("a"));
        }
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
