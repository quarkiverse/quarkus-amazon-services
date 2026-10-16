package io.quarkiverse.amazon.codegen.emitters.tasks;

import java.util.ArrayList;
import java.util.List;

import io.quarkiverse.amazon.codegen.model.QuarkusServiceModel;
import io.quarkiverse.amazon.codegen.poet.deployment.TestBeanTestClass;
import io.quarkiverse.amazon.codegen.poet.deployment.TestProcessorClass;
import io.quarkiverse.amazon.codegen.poet.deployment.TestSyntheticBeanTestClass;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;

public class DeploymentTestGeneratorTasks extends BaseGeneratorTasks {

    public DeploymentTestGeneratorTasks(GeneratorTaskParams dependencies, QuarkusServiceModel quarkusModel) {
        super(dependencies, quarkusModel, false);
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        List<GeneratorTask> generatorTasks = new ArrayList<>();
        addPoetGeneratorTask(generatorTasks, new TestProcessorClass(quarkusModel));
        addPoetGeneratorTask(generatorTasks, new TestBeanTestClass(quarkusModel));
        addPoetGeneratorTask(generatorTasks, new TestSyntheticBeanTestClass(quarkusModel));

        return generatorTasks;
    }
}
