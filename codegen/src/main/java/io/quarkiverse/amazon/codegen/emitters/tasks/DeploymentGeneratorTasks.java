package io.quarkiverse.amazon.codegen.emitters.tasks;

import java.util.ArrayList;
import java.util.List;

import io.quarkiverse.amazon.codegen.model.QuarkusServiceModel;
import io.quarkiverse.amazon.codegen.poet.deployment.ProcessorClass;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;

public class DeploymentGeneratorTasks extends BaseGeneratorTasks {

    public DeploymentGeneratorTasks(GeneratorTaskParams dependencies, QuarkusServiceModel quarkusModel) {
        super(dependencies, quarkusModel);
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        List<GeneratorTask> generatorTasks = new ArrayList<>();
        addPoetGeneratorTask(generatorTasks, new ProcessorClass(quarkusModel));

        return generatorTasks;
    }
}
