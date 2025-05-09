package io.quarkiverse.amazon.codegen.emitters.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.quarkiverse.amazon.codegen.poet.runtime.BuildTimeConfigClass;
import io.quarkiverse.amazon.codegen.poet.runtime.ConfigClass;
import io.quarkiverse.amazon.codegen.poet.runtime.RecorderClass;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;

public class RuntimeGeneratorTasks extends BaseGeneratorTasks {

    public RuntimeGeneratorTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        List<GeneratorTask> generatorTasks = new ArrayList<>();
        generatorTasks.add(createRecorderClassTask());
        generatorTasks.add(createConfigClassTask());
        generatorTasks.add(createBuildTimeConfigTask());

        return generatorTasks;
    }

    private GeneratorTask createRecorderClassTask() throws IOException {
        return createPoetGeneratorTaskWithEmptyHeader(new RecorderClass(model));
    }

    private GeneratorTask createConfigClassTask() throws IOException {
        return createPoetGeneratorTaskWithEmptyHeader(new ConfigClass(model));
    }

    private GeneratorTask createBuildTimeConfigTask() throws IOException {
        return createPoetGeneratorTaskWithEmptyHeader(new BuildTimeConfigClass(model));
    }
}