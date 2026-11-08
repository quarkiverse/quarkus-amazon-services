package io.quarkiverse.amazon.common.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.WithName;
import io.smallrye.config.WithUnnamedKey;
import io.smallrye.config.common.MapBackedConfigSource;

class ConfigSourceMappingTest {
    interface Paths {
        @WithUnnamedKey("default")
        Map<String, String> path();
    }

    interface AppConfig {
        Optional<String> application();

        Map<String, Profile> entries();

        interface Profile {
            String application();
        }
    }

    interface LegacyAndPrefixedPaths {
        Optional<String> path();

        @WithName("path")
        Map<String, String> prefixedPaths();
    }

    @Test
    void scalarPathAndPrefixedPathsCanBeMappedTogether() {
        var mapping = new SmallRyeConfigBuilder().withMapping(LegacyAndPrefixedPaths.class, "test.ssm")
                .withDefaultValue("test.ssm.path", "/legacy")
                .withDefaultValue("test.ssm.path.\"myapp.db\"", "/database")
                .build().getConfigMapping(LegacyAndPrefixedPaths.class, "test.ssm");
        assertEquals(Optional.of("/legacy"), mapping.path());
        assertEquals(Map.of("myapp.db", "/database"), mapping.prefixedPaths());
    }

    interface Databases {
        Map<String, String> databases();
    }

    @Test
    void unnamedScalarMapDoesNotCollectLegacyPath() {
        var config = new SmallRyeConfigBuilder().withMapping(Paths.class, "test.ssm")
                .withSources(new MapBackedConfigSource("test", Map.of(
                        "test.ssm.path", "/legacy",
                        "test.ssm.path.\"myapp.db\"", "/database")) {
                }).build();

        assertEquals(Map.of("myapp.db", "/database"),
                config.getConfigMapping(Paths.class, "test.ssm").path());
        assertEquals("/legacy", config.getValue("test.ssm.path", String.class));
    }

    @Test
    void legacyAppConfigPropertiesDoNotBecomeNamedEntries() {
        var config = new SmallRyeConfigBuilder().withMapping(AppConfig.class, "test.appconfig")
                .withSources(new MapBackedConfigSource("test", Map.of("test.appconfig.application", "legacy")) {
                }).build();

        assertEquals(Optional.of("legacy"), config.getConfigMapping(AppConfig.class, "test.appconfig").application());
        assertTrue(config.getConfigMapping(AppConfig.class, "test.appconfig").entries().isEmpty());
    }

    @Test
    void mapDiscoveryRequiresPropertyNames() {
        var visible = new MapBackedConfigSource("visible", Map.of("test.databases.main", "db")) {
        };
        var hidden = new MapBackedConfigSource("hidden", visible.getProperties()) {
            @Override
            public Set<String> getPropertyNames() {
                return Set.of();
            }
        };

        assertEquals(Map.of("main", "db"), new SmallRyeConfigBuilder().withMapping(Databases.class, "test")
                .withSources(visible).build().getConfigMapping(Databases.class, "test").databases());
        assertTrue(new SmallRyeConfigBuilder().withMapping(Databases.class, "test")
                .withSources(hidden).build().getConfigMapping(Databases.class, "test").databases().isEmpty());
        assertEquals("db", hidden.getValue("test.databases.main"));
    }
}
