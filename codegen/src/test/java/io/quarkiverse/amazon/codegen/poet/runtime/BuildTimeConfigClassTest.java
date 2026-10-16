package io.quarkiverse.amazon.codegen.poet.runtime;

import static io.quarkiverse.amazon.codegen.poet.ClientTestModels.customNamesConfig;
import static io.quarkiverse.amazon.codegen.poet.ClientTestModels.restJsonC2jModels;
import static io.quarkiverse.amazon.codegen.poet.ClientTestModels.restJsonQuarkusModel;
import static io.quarkiverse.amazon.codegen.poet.PoetMatchers.generatesTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkiverse.amazon.codegen.QuarkusCustomizationConfig;
import io.quarkiverse.amazon.codegen.model.QuarkusServiceModel;
import io.quarkiverse.amazon.codegen.model.QuarkusServiceModelBuilder;

public class BuildTimeConfigClassTest {

    @TempDir
    Path sourcesDirectory;

    @Test
    public void testBuildTimeConfigClass() {
        validateGeneration(restJsonQuarkusModel(), "test-buildtimeconfig-class.java");
    }

    @Test
    public void testBuildTimeConfigClassWithCustomDevServices() throws IOException {
        provideSource("io/quarkiverse/amazon/ecr/runtime/EcrDevServicesBuildTimeConfig.java");

        validateGeneration(quarkusModel(QuarkusCustomizationConfig.create()),
                "test-buildtimeconfig-class-customdevservices.java");
    }

    @Test
    public void testBuildTimeConfigClassWithCustomNames() throws IOException {
        provideSource("io/quarkiverse/amazon/myecr/runtime/MyEcrDevServicesBuildTimeConfig.java");

        validateGeneration(quarkusModel(customNamesConfig()), "test-buildtimeconfig-class-customnames.java");
    }

    private void provideSource(String path) throws IOException {
        Path source = sourcesDirectory.resolve(path);
        Files.createDirectories(source.getParent());
        Files.writeString(source, "");
    }

    private QuarkusServiceModel quarkusModel(QuarkusCustomizationConfig config) {
        return new QuarkusServiceModelBuilder(restJsonC2jModels())
                .quarkusCustomizationConfig(config)
                .sourcesDirectory(sourcesDirectory.toString())
                .build();
    }

    static void validateGeneration(QuarkusServiceModel model, String expectedClassName) {
        assertThat(new BuildTimeConfigClass(model), generatesTo(expectedClassName));
    }
}
