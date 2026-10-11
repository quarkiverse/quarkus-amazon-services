package io.quarkiverse.amazon.codegen;

import io.quarkiverse.amazon.codegen.emitters.tasks.DeploymentTestGeneratorTasks;
import software.amazon.awssdk.codegen.CodeGenerator;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;

public class DeploymentTestCodeGenerator extends BaseCodeGenerator {

    public DeploymentTestCodeGenerator(BaseBuilder builder) {
        super(builder);
    }

    @Override
    GeneratorTask createGeneratorTasks(IntermediateModel intermediateModel, boolean hasCustomDevServicesBuildTimeConfig) {
        return new DeploymentTestGeneratorTasks(GeneratorTaskParams.create(intermediateModel,
                generatedSourcesDirectory,
                generatedTestsDirectory,
                generatedResourcesDirectory));
    }

    /**
     * @return Builder instance to construct a {@link CodeGenerator}.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseBuilder {

        @Override
        public BaseCodeGenerator build() {
            return new DeploymentTestCodeGenerator(this);
        }
    }
}
