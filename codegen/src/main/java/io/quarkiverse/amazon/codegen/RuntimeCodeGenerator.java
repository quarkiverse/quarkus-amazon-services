package io.quarkiverse.amazon.codegen;

import java.nio.file.Paths;

import io.quarkiverse.amazon.codegen.docs.DocsMetadataGenerator;
import io.quarkiverse.amazon.codegen.emitters.tasks.RuntimeGeneratorTasks;
import io.quarkiverse.amazon.codegen.extension.ExtensionDescriptorGenerator;
import io.quarkiverse.amazon.codegen.model.QuarkusServiceModel;
import software.amazon.awssdk.codegen.CodeGenerator;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;

public class RuntimeCodeGenerator extends BaseCodeGenerator {

    private final String docsMetadataDirectory;
    private final String extensionName;

    public RuntimeCodeGenerator(Builder builder) {
        super(builder);
        this.docsMetadataDirectory = builder.docsMetadataDirectory;
        this.extensionName = builder.extensionName;
    }

    @Override
    GeneratorTask createGeneratorTasks(QuarkusServiceModel model) {
        return new RuntimeGeneratorTasks(GeneratorTaskParams.create(model.intermediateModel(),
                generatedSourcesDirectory,
                generatedTestsDirectory,
                generatedResourcesDirectory),
                model);
    }

    @Override
    protected void postGenerate(QuarkusServiceModel model) {
        new ExtensionDescriptorGenerator(model, extensionName).write(Paths.get(generatedResourcesDirectory));
        if (docsMetadataDirectory != null) {
            new DocsMetadataGenerator(model, extensionName).write(Paths.get(docsMetadataDirectory));
        }
    }

    /**
     * @return Builder instance to construct a {@link CodeGenerator}.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseCodeGenerator.BaseBuilder {

        private String docsMetadataDirectory;
        private String extensionName;

        /**
         * Directory where the documentation metadata of the service is written. Nothing is written if not set.
         */
        public Builder docsMetadataDirectory(String docsMetadataDirectory) {
            this.docsMetadataDirectory = docsMetadataDirectory;
            return this;
        }

        public Builder extensionName(String extensionName) {
            this.extensionName = extensionName;
            return this;
        }

        @Override
        public BaseCodeGenerator build() {
            return new RuntimeCodeGenerator(this);
        }
    }
}
