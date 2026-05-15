package io.quarkiverse.amazon.codegen.emitters.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.quarkiverse.amazon.codegen.poet.deployment.TestBeanTestClass;
import io.quarkiverse.amazon.codegen.poet.deployment.TestProcessorClass;
import io.quarkiverse.amazon.codegen.poet.deployment.TestSyntheticBeanTestClass;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;

public class DeploymentTestGeneratorTasks extends BaseGeneratorTasks {

    public DeploymentTestGeneratorTasks(GeneratorTaskParams dependencies) {
        super(dependencies, false);
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        List<GeneratorTask> generatorTasks = new ArrayList<>();
        generatorTasks.add(createTestProcessorClassTask());
        generatorTasks.add(createTestBeanTestClassTask());
        generatorTasks.add(createTestSyntheticBeanTestClassTask());

        return generatorTasks;
    }

    private GeneratorTask createTestProcessorClassTask() throws IOException {
        return createPoetGeneratorTaskWithEmptyHeader(new TestProcessorClass(model));
    }

    private GeneratorTask createTestBeanTestClassTask() throws IOException {
        return createPoetGeneratorTaskWithEmptyHeader(new TestBeanTestClass(model));
    }

    private GeneratorTask createTestSyntheticBeanTestClassTask() throws IOException {
        return createPoetGeneratorTaskWithEmptyHeader(new TestSyntheticBeanTestClass(model));
    }
}
