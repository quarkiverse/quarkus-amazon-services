package io.quarkiverse.amazon.codegen.emitters.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.quarkiverse.amazon.codegen.poet.runtime.TestBeanClass;
import io.quarkiverse.amazon.codegen.poet.runtime.TestRecorderClass;
import io.quarkiverse.amazon.codegen.poet.runtime.TestSyntheticBeanClass;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;

public class RuntimeTestGeneratorTasks extends BaseGeneratorTasks {

    public RuntimeTestGeneratorTasks(GeneratorTaskParams dependencies) {
        super(dependencies, false);
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        List<GeneratorTask> generatorTasks = new ArrayList<>();
        generatorTasks.add(createTestRecorderClassTask());
        generatorTasks.add(createTestBeanClassTask());
        generatorTasks.add(createTestSyntheticBeanClassTask());

        return generatorTasks;
    }

    private GeneratorTask createTestRecorderClassTask() throws IOException {
        return createPoetGeneratorTaskWithEmptyHeader(new TestRecorderClass(model));
    }

    private GeneratorTask createTestBeanClassTask() throws IOException {
        return createPoetGeneratorTaskWithEmptyHeader(new TestBeanClass(model));
    }

    private GeneratorTask createTestSyntheticBeanClassTask() throws IOException {
        return createPoetGeneratorTaskWithEmptyHeader(new TestSyntheticBeanClass(model));
    }
}
