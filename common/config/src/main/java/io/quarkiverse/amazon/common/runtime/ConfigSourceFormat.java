package io.quarkiverse.amazon.common.runtime;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import jakarta.json.JsonValue;

import io.smallrye.config.source.yaml.YamlConfigSource;

/** Text formats supported by the AWS configuration sources. */
public enum ConfigSourceFormat {
    AUTO,
    RAW,
    JSON,
    PROPERTIES,
    YAML;

    private static final boolean YAML_AVAILABLE = yamlAvailable();

    private static boolean yamlAvailable() {
        try {
            Class.forName("io.smallrye.config.source.yaml.YamlConfigSource", false, ConfigSourceFormat.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError unavailable) {
            return false;
        }
    }

    /** Checks optional format dependencies before a factory creates its AWS client. */
    public void checkAvailable() {
        if (this == YAML) {
            decode("", "");
        }
    }

    public Map<String, String> decode(String prefix, String content) {
        if (content == null) {
            return Map.of();
        }
        return switch (this) {
            case AUTO -> JsonConfigFlattener.expandValue(prefix, content);
            case RAW -> {
                if (prefix.isEmpty()) {
                    throw new IllegalArgumentException("Raw configuration requires a non-empty target name.");
                }
                yield Map.of(prefix, content);
            }
            case JSON -> {
                JsonValue value = JsonConfigFlattener.parseJson(content);
                if (prefix.isEmpty() && value != JsonValue.NULL
                        && value.getValueType() != JsonValue.ValueType.OBJECT
                        && value.getValueType() != JsonValue.ValueType.ARRAY) {
                    throw new IllegalArgumentException("JSON scalar configuration requires a non-empty target name.");
                }
                Map<String, String> result = new LinkedHashMap<>();
                JsonConfigFlattener.flattenJson(prefix, value, result);
                yield result;
            }
            case YAML -> {
                // Native analysis must eliminate the optional class reference when YAML is absent.
                if (!YAML_AVAILABLE) {
                    throw new IllegalArgumentException("YAML configuration requires the quarkus-config-yaml extension.");
                }
                try {
                    Map<String, String> result = new LinkedHashMap<>();
                    new YamlConfigSource("AWS configuration", content).getProperties().forEach((name, value) -> result.put(
                            prefix.isEmpty() ? name : prefix + (name.startsWith("[") ? "" : ".") + name, value));
                    yield result;
                } catch (NoClassDefFoundError e) {
                    throw new IllegalArgumentException("YAML configuration requires the quarkus-config-yaml extension.");
                } catch (RuntimeException | StackOverflowError e) {
                    throw new IllegalArgumentException("Invalid YAML configuration content.");
                }
            }
            case PROPERTIES -> {
                Properties properties = new Properties();
                try {
                    properties.load(new StringReader(content));
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid Java properties configuration content.");
                }
                Map<String, String> result = new LinkedHashMap<>();
                for (String name : properties.stringPropertyNames()) {
                    result.put(prefix.isEmpty() ? name : prefix + "." + name, properties.getProperty(name));
                }
                yield result;
            }
        };
    }
}
