package io.quarkiverse.amazon.common.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

public class JsonConfigFlattenerTest {

    @Test
    void testIsJsonValue_WithJsonObject() {
        assertTrue(JsonConfigFlattener.isJsonValue("{\"key\":\"value\"}"));
        assertTrue(JsonConfigFlattener.isJsonValue("  {\"key\":\"value\"}"));
    }

    @Test
    void testIsJsonValue_WithJsonArray() {
        assertTrue(JsonConfigFlattener.isJsonValue("[1,2,3]"));
        assertTrue(JsonConfigFlattener.isJsonValue("  [1,2,3]"));
    }

    @Test
    void testIsJsonValue_WithPlainString() {
        assertFalse(JsonConfigFlattener.isJsonValue("plaintext"));
        assertFalse(JsonConfigFlattener.isJsonValue("key=value"));
    }

    @Test
    void testIsJsonValue_WithNull() {
        assertFalse(JsonConfigFlattener.isJsonValue(null));
    }

    @Test
    void testIsJsonValue_WithEmpty() {
        assertFalse(JsonConfigFlattener.isJsonValue(""));
    }

    @Test
    void testExpandValue_PlainString() {
        Map<String, String> result = JsonConfigFlattener.expandValue("my.prefix", "plaintext");
        assertEquals(1, result.size());
        assertEquals("plaintext", result.get("my.prefix"));
    }

    @Test
    void testExpandValue_JsonObject() {
        String json = "{\"host\":\"localhost\",\"port\":5432}";
        Map<String, String> result = JsonConfigFlattener.expandValue("db", json);

        assertEquals(2, result.size());
        assertEquals("localhost", result.get("db.host"));
        assertEquals("5432", result.get("db.port"));
    }

    @Test
    void testExpandValue_JsonArray() {
        String json = "[\"item1\",\"item2\",\"item3\"]";
        Map<String, String> result = JsonConfigFlattener.expandValue("items", json);

        assertEquals(3, result.size());
        assertEquals("item1", result.get("items[0]"));
        assertEquals("item2", result.get("items[1]"));
        assertEquals("item3", result.get("items[2]"));
    }

    @Test
    void testExpandValue_NestedJsonObject() {
        String json = "{\"database\":{\"host\":\"localhost\",\"port\":5432},\"name\":\"mydb\"}";
        Map<String, String> result = JsonConfigFlattener.expandValue("app", json);

        assertEquals(3, result.size());
        assertEquals("localhost", result.get("app.database.host"));
        assertEquals("5432", result.get("app.database.port"));
        assertEquals("mydb", result.get("app.name"));
    }

    @Test
    void testExpandValue_ArrayOfObjects() {
        String json = "[{\"name\":\"s1\",\"ip\":\"1.1.1.1\"},{\"name\":\"s2\",\"ip\":\"2.2.2.2\"}]";
        Map<String, String> result = JsonConfigFlattener.expandValue("servers", json);

        assertEquals(4, result.size());
        assertEquals("s1", result.get("servers[0].name"));
        assertEquals("1.1.1.1", result.get("servers[0].ip"));
        assertEquals("s2", result.get("servers[1].name"));
        assertEquals("2.2.2.2", result.get("servers[1].ip"));
    }

    @Test
    void testExpandValue_JsonObjectWithBooleans() {
        String json = "{\"enabled\":true,\"debug\":false}";
        Map<String, String> result = JsonConfigFlattener.expandValue("config", json);

        assertEquals(2, result.size());
        assertEquals("true", result.get("config.enabled"));
        assertEquals("false", result.get("config.debug"));
    }

    @Test
    void testExpandValue_JsonObjectWithNull() {
        String json = "{\"value\":null}";
        Map<String, String> result = JsonConfigFlattener.expandValue("config", json);

        assertEquals(1, result.size());
        assertEquals("null", result.get("config.value"));
    }

    @Test
    void testExpandValue_EmptyJsonObject() {
        String json = "{}";
        Map<String, String> result = JsonConfigFlattener.expandValue("config", json);

        // Empty JSON should fall back to raw value
        assertEquals(1, result.size());
        assertEquals("{}", result.get("config"));
    }

    @Test
    void testExpandValue_EmptyJsonArray() {
        String json = "[]";
        Map<String, String> result = JsonConfigFlattener.expandValue("items", json);

        // Empty JSON array should fall back to raw value
        assertEquals(1, result.size());
        assertEquals("[]", result.get("items"));
    }

    @Test
    void testExpandValue_InvalidJson() {
        String json = "{invalid json";
        Map<String, String> result = JsonConfigFlattener.expandValue("config", json);

        // Invalid JSON should fall back to raw value
        assertEquals(1, result.size());
        assertEquals("{invalid json", result.get("config"));
    }

    @Test
    void testExpandValue_NullPrefix() {
        Map<String, String> result = JsonConfigFlattener.expandValue(null, "value");
        assertEquals(0, result.size());
    }

    @Test
    void testExpandValue_EmptyPrefix() {
        Map<String, String> result = JsonConfigFlattener.expandValue("", "value");
        assertEquals(0, result.size());
    }

    @Test
    void testExpandValue_NullValue() {
        Map<String, String> result = JsonConfigFlattener.expandValue("prefix", null);
        assertEquals(0, result.size());
    }

    @Test
    void testExpandValue_EmptyValue() {
        Map<String, String> result = JsonConfigFlattener.expandValue("prefix", "");
        assertEquals(0, result.size());
    }

    @Test
    void testExpandValue_WhitespaceOnly() {
        Map<String, String> result = JsonConfigFlattener.expandValue("prefix", "   ");
        assertEquals(0, result.size());
    }

    @Test
    void testExpandValue_ComplexNestedStructure() {
        String json = "{\"app\":{\"name\":\"myapp\",\"services\":[{\"name\":\"api\",\"port\":8080},{\"name\":\"web\",\"port\":3000}]}}";
        Map<String, String> result = JsonConfigFlattener.expandValue("config", json);

        assertEquals(5, result.size());
        assertEquals("myapp", result.get("config.app.name"));
        assertEquals("api", result.get("config.app.services[0].name"));
        assertEquals("8080", result.get("config.app.services[0].port"));
        assertEquals("web", result.get("config.app.services[1].name"));
        assertEquals("3000", result.get("config.app.services[1].port"));
    }

    @Test
    void testExpandValue_JsonNumbers() {
        String json = "{\"integer\":42,\"float\":3.14,\"negative\":-100}";
        Map<String, String> result = JsonConfigFlattener.expandValue("nums", json);

        assertEquals(3, result.size());
        assertEquals("42", result.get("nums.integer"));
        assertEquals("3.14", result.get("nums.float"));
        assertEquals("-100", result.get("nums.negative"));
    }

    @Test
    void testExpandValue_JsonWithSpecialCharactersInString() {
        String json = "{\"text\":\"hello\\nworld\",\"quote\":\"say \\\"hi\\\"\"}";
        Map<String, String> result = JsonConfigFlattener.expandValue("data", json);

        assertEquals(2, result.size());
        assertTrue(result.containsKey("data.text"));
        assertTrue(result.containsKey("data.quote"));
    }
}
