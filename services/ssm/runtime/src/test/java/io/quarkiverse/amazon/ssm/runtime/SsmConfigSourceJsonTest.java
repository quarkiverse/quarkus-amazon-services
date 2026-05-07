package io.quarkiverse.amazon.ssm.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

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
}
