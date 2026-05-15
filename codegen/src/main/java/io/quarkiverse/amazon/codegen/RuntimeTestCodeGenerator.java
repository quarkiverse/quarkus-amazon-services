package io.quarkiverse.amazon.codegen;

import io.quarkiverse.amazon.codegen.emitters.tasks.RuntimeTestGeneratorTasks;
import software.amazon.awssdk.codegen.CodeGenerator;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;

public class RuntimeTestCodeGenerator extends BaseCodeGenerator {

    public RuntimeTestCodeGenerator(BaseBuilder builder) {
        super(builder);
    }

    @Override
    GeneratorTask createGeneratorTasks(IntermediateModel intermediateModel) {
        return new RuntimeTestGeneratorTasks(GeneratorTaskParams.create(intermediateModel,
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

    public static class Builder extends BaseBuilder {

        @Override
        public BaseCodeGenerator build() {
            return new RuntimeTestCodeGenerator(this);
        }
    }
}
