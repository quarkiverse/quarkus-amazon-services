package io.quarkiverse.amazon.codegen.emitters.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.quarkiverse.amazon.codegen.poet.deployment.ProcessorClass;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;

public class DeploymentGeneratorTasks extends BaseGeneratorTasks {

    public DeploymentGeneratorTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        List<GeneratorTask> generatorTasks = new ArrayList<>();
        generatorTasks.add(createProcessorClassTask());

        return generatorTasks;
    }

    private GeneratorTask createProcessorClassTask() throws IOException {
        return createPoetGeneratorTaskWithEmptyHeader(new ProcessorClass(model));
    }
}