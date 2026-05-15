package io.quarkiverse.amazon.codegen.emitters.tasks;

import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.emitters.PoetGeneratorTask;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.poet.ClassSpec;

public abstract class BaseGeneratorTasks extends software.amazon.awssdk.codegen.emitters.tasks.BaseGeneratorTasks {
    private final boolean isMain;

    public BaseGeneratorTasks(GeneratorTaskParams dependencies, boolean isMain) {
        super(dependencies);
        this.isMain = isMain;
    }

    public BaseGeneratorTasks(GeneratorTaskParams dependencies) {
        this(dependencies, true);
    }

    protected GeneratorTask createPoetGeneratorTaskWithEmptyHeader(ClassSpec classSpec) {
        String targetDirectory;
        if (isMain) {
            targetDirectory = baseDirectory + '/' + Utils.packageToDirectory(classSpec.className().packageName());
        } else {
            targetDirectory = testDirectory + '/' + Utils.packageToDirectory(classSpec.className().packageName());
        }
        return new PoetGeneratorTask(targetDirectory, "", classSpec);
    }
}
