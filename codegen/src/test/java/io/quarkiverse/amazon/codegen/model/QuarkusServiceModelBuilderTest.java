package io.quarkiverse.amazon.codegen.model;

import static io.quarkiverse.amazon.codegen.poet.ClientTestModels.customNamesConfig;
import static io.quarkiverse.amazon.codegen.poet.ClientTestModels.restJsonC2jModels;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.squareup.javapoet.ClassName;

public class QuarkusServiceModelBuilderTest {

    @TempDir
    Path sourcesDirectory;

    @Test
    public void defaultNames() {
        QuarkusServiceModel model = new QuarkusServiceModelBuilder(restJsonC2jModels()).build();

        assertThat(model.intermediateModel().getMetadata().getServiceName()).isEqualTo("Ecr");
        assertThat(model.basePackage()).isEqualTo("io.quarkiverse.amazon.ecr");
        assertThat(model.configPrefix()).isEqualTo("quarkus.ecr");
        assertThat(model.featureName()).isEqualTo("amazon-sdk-ecr");
        assertThat(model.runtimeClass("Config")).isEqualTo(ClassName.get("io.quarkiverse.amazon.ecr.runtime", "EcrConfig"));
        assertThat(model.deploymentClass("Processor"))
                .isEqualTo(ClassName.get("io.quarkiverse.amazon.ecr.deployment", "EcrProcessor"));
    }

    @Test
    public void customNames() {
        QuarkusServiceModel model = new QuarkusServiceModelBuilder(restJsonC2jModels())
                .quarkusCustomizationConfig(customNamesConfig())
                .build();

        assertThat(model.basePackage()).isEqualTo("io.quarkiverse.amazon.myecr");
        assertThat(model.configPrefix()).isEqualTo("quarkus.my-ecr");
        assertThat(model.featureName()).isEqualTo("amazon-sdk-my-ecr");
        assertThat(model.runtimeClass("Config"))
                .isEqualTo(ClassName.get("io.quarkiverse.amazon.myecr.runtime", "MyEcrConfig"));
        assertThat(model.testClass("BeanTest")).isEqualTo(ClassName.get("io.quarkiverse.amazon.myecr.test", "MyEcrBeanTest"));
    }

    @Test
    public void nothingProvidedWithoutSources() {
        QuarkusServiceModel model = new QuarkusServiceModelBuilder(restJsonC2jModels()).build();

        assertThat(model.isProvidedInSources(model.runtimeClass("Config"))).isFalse();
        assertThat(model.hasCustomDevServicesBuildTimeConfig()).isFalse();
    }

    @Test
    public void detectsProvidedSources() throws IOException {
        Path runtimePackage = Files.createDirectories(sourcesDirectory.resolve("io/quarkiverse/amazon/ecr/runtime"));
        Files.writeString(runtimePackage.resolve("EcrConfig.java"), "");

        QuarkusServiceModel model = new QuarkusServiceModelBuilder(restJsonC2jModels())
                .sourcesDirectory(sourcesDirectory.toString())
                .build();

        assertThat(model.isProvidedInSources(model.runtimeClass("Config"))).isTrue();
        assertThat(model.isProvidedInSources(model.runtimeClass("Recorder"))).isFalse();
        assertThat(model.hasCustomDevServicesBuildTimeConfig()).isFalse();
    }

    @Test
    public void detectsCustomDevServicesBuildTimeConfig() throws IOException {
        Path runtimePackage = Files.createDirectories(sourcesDirectory.resolve("io/quarkiverse/amazon/ecr/runtime"));
        Files.writeString(runtimePackage.resolve("EcrDevServicesBuildTimeConfig.java"), "");

        QuarkusServiceModel model = new QuarkusServiceModelBuilder(restJsonC2jModels())
                .sourcesDirectory(sourcesDirectory.toString())
                .build();

        assertThat(model.hasCustomDevServicesBuildTimeConfig()).isTrue();
    }
}
