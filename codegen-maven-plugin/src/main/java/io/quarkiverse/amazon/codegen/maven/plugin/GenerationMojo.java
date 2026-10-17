package io.quarkiverse.amazon.codegen.maven.plugin;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import io.quarkiverse.amazon.codegen.BaseCodeGenerator.BaseBuilder;
import io.quarkiverse.amazon.codegen.DeploymentCodeGenerator;
import io.quarkiverse.amazon.codegen.DevServicesCodeGenerator;
import io.quarkiverse.amazon.codegen.RuntimeCodeGenerator;

/**
 * The Maven mojo to generate Java client code using software.amazon.awssdk:codegen module.
 */
@Mojo(name = "generate", defaultPhase = GENERATE_SOURCES)
public class GenerationMojo extends BaseGenerationMojo {

    /**
     * Directory where the runtime module writes the documentation metadata of the service.
     */
    @Parameter(property = "docsMetadataDirectory", defaultValue = "${maven.multiModuleProjectDirectory}/docs/modules/ROOT/pages/_metadata")
    private String docsMetadataDirectory;

    @Override
    BaseBuilder getCodeGeneratorBuilder(MavenProject project) {
        if (isDevServicesModule(project)) {
            return DevServicesCodeGenerator.builder();
        }
        if (project.getArtifactId().endsWith("-deployment")) {
            return DeploymentCodeGenerator.builder();
        }

        // the extension name is the name of the service folder holding the runtime, deployment and devservices modules
        return RuntimeCodeGenerator.builder()
                .docsMetadataDirectory(docsMetadataDirectory)
                .extensionName(project.getParent().getBasedir().getName());
    }
}
