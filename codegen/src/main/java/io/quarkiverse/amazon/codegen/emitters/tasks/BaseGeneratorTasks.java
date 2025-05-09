package io.quarkiverse.amazon.codegen.emitters.tasks;

import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.emitters.PoetGeneratorTask;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.poet.ClassSpec;

public abstract class BaseGeneratorTasks extends software.amazon.awssdk.codegen.emitters.tasks.BaseGeneratorTasks {
    public BaseGeneratorTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
    }

    protected GeneratorTask createPoetGeneratorTaskWithEmptyHeader(ClassSpec classSpec) {
        String targetDirectory = baseDirectory + '/' + Utils.packageToDirectory(classSpec.className().packageName());
        return new PoetGeneratorTask(targetDirectory, "", classSpec);
    }
}
