package io.quarkiverse.amazon.codegen;

import static io.quarkiverse.amazon.codegen.poet.ClientTestModels.restJsonC2jModels;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class RuntimeCodeGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    public void generateAllClasses() throws IOException {
        Path sources = Files.createDirectories(tempDir.resolve("src"));
        Path generated = tempDir.resolve("generated");

        generate(sources, generated);

        Path runtimePackage = generated.resolve("io/quarkiverse/amazon/ecr/runtime");
        assertThat(runtimePackage.resolve("EcrConfig.java")).exists();
        assertThat(runtimePackage.resolve("EcrRecorder.java")).exists();
        assertThat(runtimePackage.resolve("EcrBuildTimeConfig.java")).exists();
    }

    @Test
    public void skipClassesProvidedInSources() throws IOException {
        Path sources = tempDir.resolve("src");
        Path sourcesRuntimePackage = Files.createDirectories(sources.resolve("io/quarkiverse/amazon/ecr/runtime"));
        Files.writeString(sourcesRuntimePackage.resolve("EcrConfig.java"), "");
        Path generated = tempDir.resolve("generated");

        generate(sources, generated);

        Path runtimePackage = generated.resolve("io/quarkiverse/amazon/ecr/runtime");
        assertThat(runtimePackage.resolve("EcrConfig.java")).doesNotExist();
        assertThat(runtimePackage.resolve("EcrRecorder.java")).exists();
        assertThat(runtimePackage.resolve("EcrBuildTimeConfig.java")).exists();
    }

    private void generate(Path sources, Path generated) {
        RuntimeCodeGenerator.builder()
                .models(restJsonC2jModels())
                .generatedSourcesDirectory(generated.toString())
                .generatedTestsDirectory(tempDir.resolve("generated-tests").toString())
                .sourcesDirectory(sources.toString())
                .build()
                .execute();
    }
}
