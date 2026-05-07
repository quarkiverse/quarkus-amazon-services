package io.quarkiverse.amazon.secretsmanager.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Basic validation tests for SecretsManagerConfigSource JSON parsing.
 * Note: Full integration tests with AWS SDK mocking are in the integration-tests module.
 * These tests validate the core JSON flattening logic without dealing with SDK method overloading.
 */
public class SecretsManagerConfigSourceTest {

    @Test
    void testJsonFlattenerIsAvailable() {
        // Verify the JsonConfigFlattener utility is available and working
        assertTrue(io.quarkiverse.amazon.common.runtime.JsonConfigFlattener.isJsonValue("{\"key\":\"value\"}"));
    }

    @Test
    void testJsonObjectFlattening() {
        String json = "{\"host\":\"localhost\",\"port\":5432}";
        var result = io.quarkiverse.amazon.common.runtime.JsonConfigFlattener.expandValue("db", json);

        assertEquals("localhost", result.get("db.host"));
        assertEquals("5432", result.get("db.port"));
    }

    @Test
    void testJsonArrayFlattening() {
        String json = "[\"item1\",\"item2\"]";
        var result = io.quarkiverse.amazon.common.runtime.JsonConfigFlattener.expandValue("items", json);

        assertEquals("item1", result.get("items[0]"));
        assertEquals("item2", result.get("items[1]"));
    }

    @Test
    void testNestedJsonFlattening() {
        String json = "{\"database\":{\"host\":\"localhost\",\"port\":5432}}";
        var result = io.quarkiverse.amazon.common.runtime.JsonConfigFlattener.expandValue("app", json);

        assertEquals("localhost", result.get("app.database.host"));
        assertEquals("5432", result.get("app.database.port"));
    }

    @Test
    void testInvalidJsonFallback() {
        String badJson = "{invalid json";
        var result = io.quarkiverse.amazon.common.runtime.JsonConfigFlattener.expandValue("config", badJson);

        assertEquals(badJson, result.get("config"));
    }

    @Test
    void testPlainStringNoFlattening() {
        String plain = "plaintext-value";
        var result = io.quarkiverse.amazon.common.runtime.JsonConfigFlattener.expandValue("secret", plain);

        assertEquals(plain, result.get("secret"));
    }
}
