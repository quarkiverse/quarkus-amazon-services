package io.quarkiverse.amazon.codegen.maven.plugin;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;

import io.quarkiverse.amazon.codegen.BaseCodeGenerator.BaseBuilder;
import io.quarkiverse.amazon.codegen.DeploymentCodeGenerator;
import io.quarkiverse.amazon.codegen.RuntimeCodeGenerator;

/**
 * The Maven mojo to generate Java client code using software.amazon.awssdk:codegen module.
 */
@Mojo(name = "generate", defaultPhase = GENERATE_SOURCES)
public class GenerationMojo extends BaseGenerationMojo {

    @Override
    BaseBuilder getCodeGeneratorBuilder(MavenProject project) {
        if (project.getArtifactId().endsWith("-deployment")) {
            return DeploymentCodeGenerator.builder();
        }

        return RuntimeCodeGenerator.builder();
    }
}
