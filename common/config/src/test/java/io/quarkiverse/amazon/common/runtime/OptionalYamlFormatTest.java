package io.quarkiverse.amazon.common.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

import org.junit.jupiter.api.Test;

class OptionalYamlFormatTest {
    @Test
    void missingYamlDoesNotBreakOtherFormats() throws Exception {
        String formatName = ConfigSourceFormat.class.getName();
        URL classes = ConfigSourceFormat.class.getProtectionDomain().getCodeSource().getLocation();
        try (var loader = new URLClassLoader(new URL[] { classes }, getClass().getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("io.smallrye.config.source.yaml.")) {
                    throw new ClassNotFoundException(name);
                }
                if (name.equals(formatName)) {
                    Class<?> loaded = findLoadedClass(name);
                    return loaded == null ? findClass(name) : loaded;
                }
                return super.loadClass(name, resolve);
            }
        }) {
            Class<?> formatClass = loader.loadClass(formatName);
            var decode = formatClass.getMethod("decode", String.class, String.class);
            Object raw = formatClass.getField("RAW").get(null);
            assertEquals(Map.of("target", "value"), decode.invoke(raw, "target", "value"));
            Object json = formatClass.getField("JSON").get(null);
            assertEquals(Map.of("target", "value"), decode.invoke(json, "", "{\"target\":\"value\"}"));
            Object properties = formatClass.getField("PROPERTIES").get(null);
            assertEquals(Map.of("target", "value"), decode.invoke(properties, "", "target=value"));
            Object yaml = formatClass.getField("YAML").get(null);
            var failure = assertThrows(InvocationTargetException.class,
                    () -> formatClass.getMethod("checkAvailable").invoke(yaml));
            assertEquals(IllegalArgumentException.class, failure.getCause().getClass());
            assertEquals("YAML configuration requires the quarkus-config-yaml extension.", failure.getCause().getMessage());
        }
    }
}
