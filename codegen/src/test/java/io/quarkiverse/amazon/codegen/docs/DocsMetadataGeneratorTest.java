package io.quarkiverse.amazon.codegen.docs;

import static io.quarkiverse.amazon.codegen.poet.ClientTestModels.customNamesConfig;
import static io.quarkiverse.amazon.codegen.poet.ClientTestModels.restJsonC2jModels;
import static io.quarkiverse.amazon.codegen.poet.ClientTestModels.restJsonQuarkusModel;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkiverse.amazon.codegen.QuarkusCustomizationConfig;
import io.quarkiverse.amazon.codegen.model.QuarkusServiceModel;
import io.quarkiverse.amazon.codegen.model.QuarkusServiceModelBuilder;

public class DocsMetadataGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    public void defaultValues() {
        String content = new DocsMetadataGenerator(restJsonQuarkusModel(), "ecr").content();

        assertThat(content).isEqualTo(DocsMetadataGenerator.HEADER + "\n"
                + ":service-name: ecr\n"
                + ":service-name-uppercase: Amazon ECR\n"
                + ":service-class-name: Ecr\n");
    }

    @Test
    public void attributesDifferingFromTheirDefault() {
        QuarkusServiceModel model = new QuarkusServiceModelBuilder(restJsonC2jModels())
                .quarkusCustomizationConfig(customNamesConfig())
                .build();

        String content = new DocsMetadataGenerator(model, "my-ecr-extension").content();

        assertThat(content).isEqualTo(DocsMetadataGenerator.HEADER + "\n"
                + ":service-name: my-ecr\n"
                + ":service-name-uppercase: Amazon ECR\n"
                + ":service-class-name: Ecr\n"
                + ":extension-name: my-ecr-extension\n"
                + ":sdk-package: ecr\n"
                + ":java-package: myecr\n");
    }

    @Test
    public void overrides() throws IOException {
        QuarkusCustomizationConfig config = QuarkusCustomizationConfig.create();
        config.setDisplayName("Elastic Container Registry");
        config.setDocsPageName("amazon-registry");
        QuarkusServiceModel model = new QuarkusServiceModelBuilder(restJsonC2jModels())
                .quarkusCustomizationConfig(config)
                .build();

        Path file = new DocsMetadataGenerator(model, "ecr").write(tempDir);

        assertThat(file).isEqualTo(tempDir.resolve("amazon-registry.adoc"));
        assertThat(Files.readString(file)).contains(":service-name-uppercase: Elastic Container Registry\n");
    }

    @Test
    public void deprecatedStatus() {
        QuarkusCustomizationConfig config = QuarkusCustomizationConfig.create();
        config.setStatus("deprecated");
        QuarkusServiceModel model = new QuarkusServiceModelBuilder(restJsonC2jModels())
                .quarkusCustomizationConfig(config)
                .build();

        String content = new DocsMetadataGenerator(model, "ecr").content();

        assertThat(content).endsWith(":service-class-name: Ecr\n"
                + ":extension-status: deprecated\n");
    }

    @Test
    public void defaultPageName() {
        Path file = new DocsMetadataGenerator(restJsonQuarkusModel(), "ecr").write(tempDir);

        assertThat(file).isEqualTo(tempDir.resolve("amazon-ecr.adoc"));
    }

    @Test
    public void unchangedFileIsNotRewritten() throws IOException {
        DocsMetadataGenerator generator = new DocsMetadataGenerator(restJsonQuarkusModel(), "ecr");
        Path file = generator.write(tempDir);
        FileTime past = FileTime.fromMillis(0);
        Files.setLastModifiedTime(file, past);

        generator.write(tempDir);

        assertThat(Files.getLastModifiedTime(file)).isEqualTo(past);
    }
}
