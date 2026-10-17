package io.quarkiverse.amazon.codegen.extension;

import static io.quarkiverse.amazon.codegen.poet.ClientTestModels.customNamesConfig;
import static io.quarkiverse.amazon.codegen.poet.ClientTestModels.restJsonC2jModels;
import static io.quarkiverse.amazon.codegen.poet.ClientTestModels.restJsonQuarkusModel;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkiverse.amazon.codegen.QuarkusCustomizationConfig;
import io.quarkiverse.amazon.codegen.model.QuarkusServiceModel;
import io.quarkiverse.amazon.codegen.model.QuarkusServiceModelBuilder;

public class ExtensionDescriptorGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    public void defaultValues() {
        String content = new ExtensionDescriptorGenerator(restJsonQuarkusModel(), "ecr").content();

        assertThat(content).isEqualTo(ExtensionDescriptorGenerator.HEADER + "\n"
                + "---\n"
                + "artifact: ${project.groupId}:${project.artifactId}:${project.version}\n"
                + "name: \"Amazon ECR\"\n"
                + "metadata:\n"
                + "  keywords:\n"
                + "    - \"ecr\"\n"
                + "    - \"aws\"\n"
                + "    - \"amazon\"\n"
                + "  categories:\n"
                + "    - \"cloud\"\n"
                + "  guide: https://docs.quarkiverse.io/quarkus-amazon-services/dev/amazon-ecr.html\n"
                + "  status: \"stable\"\n"
                + "  config:\n"
                + "  - \"quarkus.ecr.\"\n");
    }

    @Test
    public void customValues() {
        QuarkusCustomizationConfig config = customNamesConfig();
        config.setDisplayName("Amazon Elastic Container Registry");
        config.setDocsPageName("amazon-registry");
        config.setKeywords(List.of("registry", "container registry"));
        config.setCategories(List.of("cloud", "containers"));
        QuarkusServiceModel model = new QuarkusServiceModelBuilder(restJsonC2jModels())
                .quarkusCustomizationConfig(config)
                .build();

        String content = new ExtensionDescriptorGenerator(model, "my-ecr-extension").content();

        assertThat(content).contains("name: \"Amazon Elastic Container Registry\"\n")
                .contains("  keywords:\n"
                        + "    - \"my-ecr-extension\"\n"
                        + "    - \"registry\"\n"
                        + "    - \"container registry\"\n"
                        + "    - \"aws\"\n"
                        + "    - \"amazon\"\n"
                        + "  categories:\n"
                        + "    - \"cloud\"\n"
                        + "    - \"containers\"\n")
                .contains("  guide: https://docs.quarkiverse.io/quarkus-amazon-services/dev/amazon-registry.html\n")
                .contains("  - \"quarkus.my-ecr.\"\n");
    }

    @Test
    public void writtenInMetaInf() throws IOException {
        ExtensionDescriptorGenerator generator = new ExtensionDescriptorGenerator(restJsonQuarkusModel(), "ecr");

        Path file = generator.write(tempDir);

        assertThat(file).isEqualTo(tempDir.resolve("META-INF/quarkus-extension.yaml"));
        assertThat(Files.readString(file)).isEqualTo(generator.content());
    }

    @Test
    public void unchangedFileIsNotRewritten() throws IOException {
        ExtensionDescriptorGenerator generator = new ExtensionDescriptorGenerator(restJsonQuarkusModel(), "ecr");
        Path file = generator.write(tempDir);
        FileTime past = FileTime.fromMillis(0);
        Files.setLastModifiedTime(file, past);

        generator.write(tempDir);

        assertThat(Files.getLastModifiedTime(file)).isEqualTo(past);
    }
}
