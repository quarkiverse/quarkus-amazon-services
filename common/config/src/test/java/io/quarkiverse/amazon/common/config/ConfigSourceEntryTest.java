package io.quarkiverse.amazon.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.amazon.common.runtime.ConfigSourceFormat;

class ConfigSourceEntryTest {
    private final ResourceId id = new ResourceId("document");

    @ParameterizedTest
    @ValueSource(strings = { "text/plain", "application/json", "application/yaml" })
    void documentAutoPrioritizesJsonOverMime(String mime) {
        assertEquals(Map.of("feature.enabled", "true"),
                decode("{\"feature\":{\"enabled\":true}}", mime, ConfigSourceFormat.AUTO));
    }

    @ParameterizedTest
    @ValueSource(strings = { "application/yaml", "text/yaml", "Application/YAML; charset=utf-8" })
    void documentAutoAcceptsNestedAndFlowYaml(String mime) {
        assertEquals(Map.of("feature.enabled", "true"), decode("feature:\n  enabled: true\n", mime, ConfigSourceFormat.AUTO));
        assertEquals(Map.of("feature", "true"), decode("{feature: true}", mime, ConfigSourceFormat.AUTO));
    }

    @ParameterizedTest
    @ValueSource(strings = { "null", "true", "123", "\"abc\"", "  \"abc\"  " })
    void documentAutoRetainsLegacyScalarProperties(String content) {
        assertEquals(Map.of(content.trim(), ""), decode(content, "application/json", ConfigSourceFormat.AUTO));
    }

    @ParameterizedTest
    @ValueSource(strings = { "{invalid", "[1] trailing", "{\"a\":1} {}" })
    void malformedDocumentsAreRejected(String content) {
        assertThrows(IllegalArgumentException.class, () -> decode(content, "text/plain", ConfigSourceFormat.AUTO));
    }

    @Test
    void explicitFormatOverridesMimeAndAutoDetection() {
        assertEquals(Map.of("{key", "value"), decode("{key=value", "application/yaml", ConfigSourceFormat.PROPERTIES));
        assertThrows(IllegalArgumentException.class,
                () -> decode("feature: true", "application/yaml", ConfigSourceFormat.JSON));
    }

    @Test
    void everyFormatUsesStrictUtf8WithoutFallback() {
        RawDocument invalid = new RawDocument(id, new byte[] { (byte) 0xff, (byte) 0xfe }, null, null);
        for (ConfigSourceFormat format : ConfigSourceFormat.values()) {
            var entry = new ConfigSourceEntry("target", id, "target", format, false);
            var error = assertThrows(IllegalArgumentException.class, () -> entry.decode(invalid));
            assertEquals("Configuration content is not valid UTF-8.", error.getMessage());
            assertNull(error.getCause());
        }
    }

    @ParameterizedTest
    @EnumSource(ConfigSourceFormat.class)
    void anInitiallyAbsentDocumentIsEmptyRegardlessOfDeclaredFormat(ConfigSourceFormat format) {
        assertEquals(Map.of(), decode("", "application/json", format));
    }

    private Map<String, String> decode(String content, String mime, ConfigSourceFormat format) {
        var entry = new ConfigSourceEntry("document", id, "", format, true);
        return entry.decode(new RawDocument(id, content.getBytes(StandardCharsets.UTF_8), mime, null));
    }
}
