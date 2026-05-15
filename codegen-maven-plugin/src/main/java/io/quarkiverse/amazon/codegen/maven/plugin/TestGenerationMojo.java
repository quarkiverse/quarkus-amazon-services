package io.quarkiverse.amazon.codegen.maven.plugin;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_TEST_SOURCES;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;

import io.quarkiverse.amazon.codegen.BaseCodeGenerator.BaseBuilder;
import io.quarkiverse.amazon.codegen.DeploymentTestCodeGenerator;
import io.quarkiverse.amazon.codegen.RuntimeTestCodeGenerator;

/**
 * The Maven mojo to generate Java client code using software.amazon.awssdk:codegen module.
 */
@Mojo(name = "generate-tests", defaultPhase = GENERATE_TEST_SOURCES)
public class TestGenerationMojo extends BaseGenerationMojo {

    @Override
    BaseBuilder getCodeGeneratorBuilder(MavenProject project) {
        if (project.getArtifactId().endsWith("-deployment")) {
            return DeploymentTestCodeGenerator.builder();
        }

        return RuntimeTestCodeGenerator.builder();
    }
}
