package io.quarkiverse.amazon.common.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConfigSourceFormatTest {
    @ParameterizedTest
    @ValueSource(strings = { "null", "true", "123", "\"abc\"", "  \"abc\"  ", "{}", "[]", "{\"a\":null}",
            " {invalid json ", "dGVzdA==", "jdbc:x://h/db?ssl=true", "url=x\nuser=y" })
    void autoPreservesCompatibleRawValues(String content) {
        assertEquals(Map.of("target", content), ConfigSourceFormat.AUTO.decode("target", content));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " \n\t " })
    void autoOmitsEmptyValues(String content) {
        assertEquals(Map.of(), ConfigSourceFormat.AUTO.decode("target", content));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " \n\t ", "dGVzdA==", "{\"a\":1}", "url=x\nuser=y" })
    void rawNeverParsesOrTrims(String content) {
        assertEquals(Map.of("target", content), ConfigSourceFormat.RAW.decode("target", content));
    }

    @ParameterizedTest
    @ValueSource(strings = { "null", "{}", "[]", "{\"a\":null}" })
    void explicitJsonOmitsNullsAndEmptyDocuments(String content) {
        assertEquals(Map.of(), ConfigSourceFormat.JSON.decode("target", content));
    }

    @ParameterizedTest
    @ValueSource(strings = { "true", "123", "\"abc\"" })
    void explicitJsonScalarsNeedATarget(String content) {
        assertEquals(Map.of("target", content.replace("\"", "")), ConfigSourceFormat.JSON.decode("target", content));
        assertThrows(IllegalArgumentException.class, () -> ConfigSourceFormat.JSON.decode("", content));
    }

    @Test
    void structuredFormatsSharePrefixRules() {
        assertEquals(Map.of("db.hosts[0].port", "42"),
                ConfigSourceFormat.JSON.decode("db", "{\"hosts\":[{\"port\":42}]}"));
        assertEquals(Map.of("db.url", "x", "db.user", "y"),
                ConfigSourceFormat.PROPERTIES.decode("db", "url=x\nuser=y"));
        assertEquals(Map.of("url", "x"), ConfigSourceFormat.PROPERTIES.decode("", "url=x"));
        assertThrows(IllegalArgumentException.class, () -> ConfigSourceFormat.RAW.decode("", "value"));
        assertEquals(Map.of("db.feature.enabled", "true", "db.hosts[0]", "one"),
                ConfigSourceFormat.YAML.decode("db", "feature:\n  enabled: true\nhosts:\n  - one\n"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "{\"a\":1} trailing", "{\"a\":1} {}", "null false", "[1] [2]", "", "  " })
    void explicitJsonRejectsAnythingAfterTheFirstValue(String content) {
        assertThrows(IllegalArgumentException.class, () -> ConfigSourceFormat.JSON.decode("target", content));
    }

    @ParameterizedTest
    @ValueSource(strings = { "{\"a\":1} trailing", "{\"a\":1} {}", "[1] [2]" })
    void autoFallsBackToTheWholeMalformedDocument(String content) {
        assertEquals(Map.of("target", content), ConfigSourceFormat.AUTO.decode("target", content));
    }

    @ParameterizedTest
    @ValueSource(strings = { "password: [secret", "a: &a\n  b: *a\n" })
    void malformedYamlHasNoUnsafeParserCause(String content) {
        var error = assertThrows(IllegalArgumentException.class, () -> ConfigSourceFormat.YAML.decode("target", content));
        assertEquals("Invalid YAML configuration content.", error.getMessage());
        assertNull(error.getCause());
    }

    @Test
    void malformedContentHasNoUnsafeParserCause() {
        for (var format : new ConfigSourceFormat[] { ConfigSourceFormat.JSON, ConfigSourceFormat.PROPERTIES }) {
            String content = format == ConfigSourceFormat.JSON ? "{\"password\":\"secret\"," : "password=\\uZZZZ";
            var error = assertThrows(IllegalArgumentException.class, () -> format.decode("target", content));
            assertNull(error.getCause());
        }
    }
}
