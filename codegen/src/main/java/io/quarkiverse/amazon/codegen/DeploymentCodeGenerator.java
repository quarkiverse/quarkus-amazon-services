package io.quarkiverse.amazon.codegen;

import io.quarkiverse.amazon.codegen.emitters.tasks.DeploymentGeneratorTasks;
import software.amazon.awssdk.codegen.CodeGenerator;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;

public class DeploymentCodeGenerator extends BaseCodeGenerator {

    public DeploymentCodeGenerator(BaseBuilder builder) {
        super(builder);
    }

    @Override
    GeneratorTask createGeneratorTasks(IntermediateModel intermediateModel) {
        return new DeploymentGeneratorTasks(GeneratorTaskParams.create(intermediateModel,
                sourcesDirectory,
                testsDirectory,
                resourcesDirectory));
    }

    /**
     * @return Builder instance to construct a {@link CodeGenerator}.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseCodeGenerator.BaseBuilder {

        @Override
        public BaseCodeGenerator build() {
            return new DeploymentCodeGenerator(this);
        }
    }
}
