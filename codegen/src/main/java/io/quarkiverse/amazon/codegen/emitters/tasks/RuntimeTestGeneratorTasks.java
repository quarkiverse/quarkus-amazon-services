package io.quarkiverse.amazon.codegen.emitters.tasks;

import java.util.ArrayList;
import java.util.List;

import io.quarkiverse.amazon.codegen.model.QuarkusServiceModel;
import io.quarkiverse.amazon.codegen.poet.runtime.TestBeanClass;
import io.quarkiverse.amazon.codegen.poet.runtime.TestRecorderClass;
import io.quarkiverse.amazon.codegen.poet.runtime.TestSyntheticBeanClass;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;

public class RuntimeTestGeneratorTasks extends BaseGeneratorTasks {

    public RuntimeTestGeneratorTasks(GeneratorTaskParams dependencies, QuarkusServiceModel quarkusModel) {
        super(dependencies, quarkusModel, false);
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        List<GeneratorTask> generatorTasks = new ArrayList<>();
        addPoetGeneratorTask(generatorTasks, new TestRecorderClass(quarkusModel));
        addPoetGeneratorTask(generatorTasks, new TestBeanClass(quarkusModel));
        addPoetGeneratorTask(generatorTasks, new TestSyntheticBeanClass(quarkusModel));

        return generatorTasks;
    }
}
