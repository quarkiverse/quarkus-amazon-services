package io.quarkiverse.amazon.common.runtime;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

/**
 * Utility class for detecting and flattening JSON values from AWS config sources.
 * <p>
 * This class provides methods to:
 * <ul>
 * <li>Detect if a value is JSON (starts with '{' or '[')</li>
 * <li>Flatten JSON objects and arrays into configuration keys with dot notation</li>
 * <li>Gracefully handle invalid JSON by returning the raw value</li>
 * </ul>
 * <p>
 * Used by both SSM and Secrets Manager ConfigSources to provide consistent JSON parsing behavior.
 */
public final class JsonConfigFlattener {

    private JsonConfigFlattener() {
        // Utility class, no instantiation
    }

    /**
     * Checks if a value looks like JSON (starts with '{' or '[').
     *
     * @param value the value to check
     * @return true if the trimmed value starts with '{' or '[', false otherwise
     */
    public static boolean isJsonValue(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return trimmed.startsWith("{") || trimmed.startsWith("[");
    }

    /**
     * Expands a parameter/secret value into zero or more configuration keys.
     * <p>
     * If the value is JSON (object or array), it is flattened with the prefix.
     * If the value is properties-like (contains '='), each entry is prefixed.
     * Otherwise, a single key with the prefix and the raw value is returned.
     * If flattening fails (invalid JSON), the raw value is returned.
     *
     * @param prefix the prefix to apply to all keys (e.g., the parameter/secret name or mapped property)
     * @param value the value to expand
     * @return a map of configuration keys to values
     */
    public static Map<String, String> expandValue(String prefix, String value) {
        if (prefix == null || prefix.isEmpty()) {
            return Map.of();
        }
        if (value == null) {
            return Map.of();
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return Map.of();
        }

        // Try to parse as JSON
        if (isJsonValue(trimmed)) {
            try (JsonReader reader = Json.createReader(new StringReader(trimmed))) {
                Map<String, String> flat = new LinkedHashMap<>();
                flattenJson("", reader.readValue(), flat);
                if (flat.isEmpty()) {
                    // Empty JSON object/array; return raw value
                    return Map.of(prefix, value);
                }
                // Build output with prefix
                Map<String, String> out = new LinkedHashMap<>();
                for (Map.Entry<String, String> e : flat.entrySet()) {
                    String k;
                    if (e.getKey().isEmpty()) {
                        k = prefix;
                    } else if (e.getKey().startsWith("[")) {
                        // Array index; no dot separator
                        k = prefix + e.getKey();
                    } else {
                        // Object key; use dot separator
                        k = prefix + "." + e.getKey();
                    }
                    out.put(k, e.getValue());
                }
                return out;
            } catch (Exception e) {
                // Failed to parse JSON; expose raw string
                return Map.of(prefix, value);
            }
        }

        // Not JSON; return as-is with prefix
        return Map.of(prefix, value);
    }

    /**
     * Recursively flattens a JSON value into configuration keys using dot notation.
     * <p>
     * Objects are flattened with dot-separated keys (e.g., "parent.child").
     * Arrays use index notation (e.g., "items[0]", "items[1]").
     * Primitives are converted to strings.
     *
     * @param keyPrefix the current prefix in the flattening process
     * @param value the JSON value to flatten
     * @param out the output map to accumulate results
     */
    private static void flattenJson(String keyPrefix, JsonValue value, Map<String, String> out) {
        switch (value.getValueType()) {
            case OBJECT:
                JsonObject obj = value.asJsonObject();
                for (String key : obj.keySet()) {
                    String p = keyPrefix.isEmpty() ? key : keyPrefix + "." + key;
                    flattenJson(p, obj.get(key), out);
                }
                break;
            case ARRAY:
                JsonArray arr = value.asJsonArray();
                for (int i = 0; i < arr.size(); i++) {
                    flattenJson(keyPrefix + "[" + i + "]", arr.get(i), out);
                }
                break;
            case STRING:
                out.put(keyPrefix, ((JsonString) value).getString());
                break;
            case NUMBER:
                out.put(keyPrefix, value.toString());
                break;
            case TRUE:
                out.put(keyPrefix, "true");
                break;
            case FALSE:
                out.put(keyPrefix, "false");
                break;
            case NULL:
                out.put(keyPrefix, "null");
                break;
        }
    }
}
