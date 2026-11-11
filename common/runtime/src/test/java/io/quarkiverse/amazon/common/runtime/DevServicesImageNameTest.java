package io.quarkiverse.amazon.common.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;

class DevServicesImageNameTest {
    private static final String PREFIX = "quarkus.aws.devservices.floci.";

    private static FlociDevServicesBuildTimeConfig config(Map<String, String> properties) {
        return new SmallRyeConfigBuilder()
                .withMapping(FlociDevServicesBuildTimeConfig.class)
                .withSources(new MapBackedConfigSource("test", properties) {
                }).build().getConfigMapping(FlociDevServicesBuildTimeConfig.class);
    }

    @Test
    void defaultsToStandardImageWithoutInitScripts() {
        var config = config(Map.of());
        assertEquals(Optional.empty(), config.imageName());
        assertEquals("floci/floci:latest", config.resolvedImageName());
    }

    @ParameterizedTest
    @ValueSource(strings = { "init-scripts-folder", "init-scripts-classpath" })
    void defaultsToCompatImageWithInitScripts(String scriptsProperty) {
        assertEquals("floci/floci:latest-compat", config(Map.of(PREFIX + scriptsProperty, "floci-init")).resolvedImageName());
    }

    @ParameterizedTest
    @ValueSource(strings = { "floci/floci:latest", "floci/floci:latest-compat", "custom/floci:custom" })
    void preservesExplicitImageWithAndWithoutInitScripts(String image) {
        var config = config(Map.of(PREFIX + "image-name", image));
        assertEquals(Optional.of(image), config.imageName());
        assertEquals(image, config.resolvedImageName());
        for (String scriptsProperty : new String[] { "init-scripts-folder", "init-scripts-classpath" }) {
            assertEquals(image, config(Map.of(PREFIX + "image-name", image,
                    PREFIX + scriptsProperty, "floci-init")).resolvedImageName());
        }
    }

    @Test
    void otherProvidersResolveTheirMappedImages() {
        var config = new SmallRyeConfigBuilder()
                .withMapping(LocalStackDevServicesBuildTimeConfig.class)
                .withMapping(MiniStackDevServicesBuildTimeConfig.class)
                .withMapping(MotoDevServicesBuildTimeConfig.class)
                .build();
        assertEquals("localstack/localstack:4.13",
                config.getConfigMapping(LocalStackDevServicesBuildTimeConfig.class).resolvedImageName());
        assertEquals("ministackorg/ministack:latest",
                config.getConfigMapping(MiniStackDevServicesBuildTimeConfig.class).resolvedImageName());
        assertEquals("motoserver/moto:latest",
                config.getConfigMapping(MotoDevServicesBuildTimeConfig.class).resolvedImageName());
    }

    @Test
    void defaultImageResolutionRejectsMissingImage() {
        var config = new SmallRyeConfigBuilder()
                .withMapping(MotoDevServicesBuildTimeConfig.class)
                .withSources(new MapBackedConfigSource("test", Map.of("quarkus.aws.devservices.moto.image-name", "")) {
                }).build().getConfigMapping(MotoDevServicesBuildTimeConfig.class);
        assertThrows(IllegalStateException.class, config::resolvedImageName);
    }
}
