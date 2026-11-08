package io.quarkiverse.amazon.ssm.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.amazon.common.runtime.ConfigSourceFormat;
import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

/**
 * Regression tests for SsmConfigSource JSON parsing.
 *
 * These tests verify that JSON parsing in SSM still works correctly after extracting
 * the flattening logic to the shared JsonConfigFlattener utility.
 */
public class SsmConfigSourceJsonTest {

    @Test
    void testSsmJsonParameterStillFlattens() {
        var result = SsmConfigSource.expandParameterValue("/app/db-config",
                "{\"host\":\"localhost\",\"port\":5432}");

        assertEquals("localhost", result.get("app.db-config.host"));
        assertEquals("5432", result.get("app.db-config.port"));
    }

    @Test
    void testSsmNestedJsonStillWorks() {
        var result = SsmConfigSource.expandParameterValue("/myapp/config",
                "{\"database\":{\"host\":\"db.example.com\",\"port\":5432},\"name\":\"mydb\"}");

        assertEquals("db.example.com", result.get("myapp.config.database.host"));
        assertEquals("5432", result.get("myapp.config.database.port"));
        assertEquals("mydb", result.get("myapp.config.name"));
    }

    @Test
    void testSsmJsonArrayStillWorks() {
        var result = SsmConfigSource.expandParameterValue("/services/servers",
                "[{\"name\":\"s1\",\"ip\":\"1.1.1.1\"},{\"name\":\"s2\",\"ip\":\"2.2.2.2\"}]");

        assertEquals("s1", result.get("services.servers[0].name"));
        assertEquals("1.1.1.1", result.get("services.servers[0].ip"));
        assertEquals("s2", result.get("services.servers[1].name"));
        assertEquals("2.2.2.2", result.get("services.servers[1].ip"));
    }

    @Test
    void testSsmInvalidJsonStillFallsBack() {
        var result = SsmConfigSource.expandParameterValue("/app/bad", "{invalid json");

        assertEquals("{invalid json", result.get("app.bad"));
    }

    @Test
    void testSsmPlainStringParameterStillWorks() {
        var result = SsmConfigSource.expandParameterValue("/app/api-key", "sk-1234567890abcdef");

        assertEquals("sk-1234567890abcdef", result.get("app.api-key"));
    }

    @Test
    void testSsmEmptyJsonObjectFallsBack() {
        var result = SsmConfigSource.expandParameterValue("/app/empty", "{}");

        assertEquals("{}", result.get("app.empty"));
    }

    @Test
    void testSsmPropertiesAreNotGuessed() {
        var result = SsmConfigSource.expandParameterValue("/app/db", "url=x\nuser=y");

        assertEquals(Map.of("app.db", "url=x\nuser=y"), result);
    }

    @Test
    void testSsmPlainValueWithoutEqualsStaysSingleKey() {
        var result = SsmConfigSource.expandParameterValue("/app/db", "plain value");

        assertEquals(Map.of("app.db", "plain value"), result);
    }

    @ParameterizedTest
    @ValueSource(strings = { "dGVzdA==", "jdbc:x://h/db?ssl=true", "null", "true", "123", "  \"abc\"  " })
    void sourcePreservesRawAutoValues(String value) {
        try (var source = source(value, ConfigSourceFormat.AUTO)) {
            assertEquals(value, source.getValue("app.db"));
        }
    }

    @Test
    void sourceUsesDeclaredPropertiesFormat() {
        try (var source = source("url=x\nuser=y", ConfigSourceFormat.PROPERTIES)) {
            assertEquals("x", source.getValue("app.db.url"));
            assertEquals("y", source.getValue("app.db.user"));
        }
    }

    @Test
    void unknownFormatFailsConfigurationMapping() {
        assertThrows(ConfigValidationException.class, () -> new SmallRyeConfigBuilder().withMapping(SsmConfigConfig.class)
                .withSources(new MapBackedConfigSource("test", Map.of("quarkus.ssm.config.format", "invalid")) {
                }).build());
    }

    private SsmConfigSource source(String value, ConfigSourceFormat format) {
        SsmClient client = mock(SsmClient.class);
        when(client.getParameters(any(GetParametersRequest.class))).thenReturn(GetParametersResponse.builder()
                .parameters(Parameter.builder().name("/app/db").value(value).build()).build());
        return new SsmConfigSource(client, null, List.of("/app/db"), true, true, Duration.ZERO, 270, format);
    }
}
