package io.quarkiverse.amazon.codegen;

import io.quarkiverse.amazon.codegen.emitters.tasks.RuntimeGeneratorTasks;
import io.quarkiverse.amazon.codegen.model.QuarkusServiceModel;
import software.amazon.awssdk.codegen.CodeGenerator;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;

public class RuntimeCodeGenerator extends BaseCodeGenerator {

    public RuntimeCodeGenerator(BaseBuilder builder) {
        super(builder);
    }

    @Override
    GeneratorTask createGeneratorTasks(QuarkusServiceModel model) {
        return new RuntimeGeneratorTasks(GeneratorTaskParams.create(model.intermediateModel(),
                generatedSourcesDirectory,
                generatedTestsDirectory,
                generatedResourcesDirectory),
                model);
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
            return new RuntimeCodeGenerator(this);
        }
    }
}
