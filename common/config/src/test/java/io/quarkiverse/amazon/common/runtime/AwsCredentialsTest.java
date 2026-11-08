package io.quarkiverse.amazon.common.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.quarkiverse.amazon.common.config.FetchFailure;

class AwsCredentialsTest {
    @Test
    void documentResolutionReadsAgainAndDoesNotRetainCredentialsAfterFailure() {
        var calls = new AtomicInteger();
        java.util.function.Function<List<String>, Map<String, String>> reader = ids -> {
            assertEquals(List.of("document"), ids);
            int call = calls.incrementAndGet();
            if (call == 2) {
                throw new AwsCredentials.ReadFailure(FetchFailure.ACCESS_DENIED);
            }
            return Map.of("document", "{\"username\":\"app\",\"password\":\"v" + call + "\"}");
        };
        assertEquals(Map.of("user", "app", "password", "v1"), AwsCredentials.resolve(Optional.of("document"), Map.of(),
                ConfigSourceFormat.JSON, "username", "password", reader));
        var failure = assertThrows(AwsCredentials.ReadFailure.class,
                () -> AwsCredentials.resolve(Optional.of("document"), Map.of(), ConfigSourceFormat.JSON,
                        "username", "password", reader));
        assertEquals(FetchFailure.ACCESS_DENIED, failure.failure());
        assertNull(failure.getMessage());
        assertNull(failure.getCause());
        assertEquals(Map.of("user", "app", "password", "v3"), AwsCredentials.resolve(Optional.of("document"), Map.of(),
                ConfigSourceFormat.JSON, "username", "password", reader));
        assertEquals(3, calls.get());
    }

    @Test
    void rawSelectionsShareReadsAndReturnOnlyImmutableCredentials() {
        var credentials = AwsCredentials.resolve(Optional.empty(), Map.of("user", "shared", "password", "shared"),
                ConfigSourceFormat.JSON, "username", "password", ids -> {
                    assertEquals(List.of("shared"), ids);
                    return Map.of("shared", " value\n${literal} ");
                });
        assertEquals(Map.of("user", " value\n${literal} ", "password", " value\n${literal} "), credentials);
        assertThrows(UnsupportedOperationException.class, () -> credentials.put("password", "other"));
    }

    @Test
    void invalidModesAndSelectionsDoNotInvokeTheReader() {
        var calls = new AtomicInteger();
        java.util.function.Function<List<String>, Map<String, String>> reader = ids -> {
            calls.incrementAndGet();
            return Map.of();
        };
        assertThrows(IllegalArgumentException.class, () -> AwsCredentials.resolve(Optional.empty(), Map.of(),
                ConfigSourceFormat.JSON, "username", "password", reader));
        assertThrows(IllegalArgumentException.class,
                () -> AwsCredentials.resolve(Optional.of("document"), Map.of("password", "raw"),
                        ConfigSourceFormat.JSON, "username", "password", reader));
        assertThrows(IllegalArgumentException.class, () -> AwsCredentials.resolve(Optional.empty(), Map.of("user", "raw"),
                ConfigSourceFormat.JSON, "username", "password", reader));
        assertThrows(IllegalArgumentException.class, () -> AwsCredentials.resolve(Optional.of("document"), Map.of(),
                ConfigSourceFormat.AUTO, "username", "password", reader));
        assertEquals(0, calls.get());
    }

    @Test
    void missingOrUnrequestedResultsRejectTheWholeRead() {
        for (Map<String, String> values : java.util.Arrays.asList(Map.<String, String> of(),
                Map.of("document", "{\"password\":\"secret\"}", "unrequested", "unsafe"), null)) {
            var failure = assertThrows(AwsCredentials.ReadFailure.class, () -> AwsCredentials.resolve(Optional.of("document"),
                    Map.of(), ConfigSourceFormat.JSON, "username", "password", ids -> values));
            assertEquals(FetchFailure.PARTIAL_RESPONSE, failure.failure());
            assertNull(failure.getMessage());
            assertNull(failure.getCause());
        }
    }

    @Test
    void formatsSelectOnlyCredentialsAndAllowPasswordOnly() {
        for (var format : new ConfigSourceFormat[] { ConfigSourceFormat.JSON, ConfigSourceFormat.PROPERTIES,
                ConfigSourceFormat.YAML }) {
            String text = switch (format) {
                case JSON -> "{\"login\":\"app\",\"pwd\":\"secret\",\"extra\":\"ignored\"}";
                case PROPERTIES -> "login=app\npwd=secret\nextra=ignored";
                default -> "login: app\npwd: secret\nextra: ignored";
            };
            var credentials = AwsCredentials.decode(text, format, "login", "pwd");
            assertEquals(Map.of("user", "app", "password", "secret"), credentials);
            assertThrows(UnsupportedOperationException.class, () -> credentials.put("password", "other"));
            assertEquals(Map.of("password", "secret"), AwsCredentials.decode(text, format, null, "pwd"));
        }
        assertEquals(Map.of("password", "secret"), AwsCredentials.decode("{\"password\":\"secret\"}",
                ConfigSourceFormat.JSON, "username", "password"));
        assertEquals(Map.of("password", " value\n${literal} "), AwsCredentials.decode(" value\n${literal} ",
                ConfigSourceFormat.RAW, "username", "password"));
    }

    @Test
    void invalidAndIncompleteValuesAreNeverCredentials() {
        assertThrows(IllegalArgumentException.class, () -> AwsCredentials.text(new byte[] { (byte) 0xff }));
        for (String json : new String[] { "{}", "[]", "null", "{broken", "{} trailing",
                "{\"password\":12}", "{\"password\":null}", "{\"password\":\"\"}",
                "{\"password\":\"\\ud800\"}", "{\"username\":false,\"password\":\"secret\"}" }) {
            assertThrows(RuntimeException.class,
                    () -> AwsCredentials.decode(json, ConfigSourceFormat.JSON, "username", "password"));
        }
        for (var format : new ConfigSourceFormat[] { ConfigSourceFormat.PROPERTIES, ConfigSourceFormat.YAML }) {
            assertThrows(IllegalArgumentException.class, () -> AwsCredentials.decode("", format, "username", "password"));
        }
        assertThrows(IllegalArgumentException.class,
                () -> AwsCredentials.decode("secret", ConfigSourceFormat.AUTO, "username", "password"));
        assertThrows(IllegalArgumentException.class, () -> AwsCredentials.validate(Map.of("expires-at", "tomorrow")));
        assertThrows(IllegalArgumentException.class, () -> AwsCredentials.validateSelection(Map.of("user", "name")));
        assertThrows(IllegalArgumentException.class,
                () -> AwsCredentials.validateSelection(Map.of("password", " ")));
    }
}
