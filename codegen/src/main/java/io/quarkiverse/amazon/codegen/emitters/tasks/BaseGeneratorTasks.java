package io.quarkiverse.amazon.codegen.emitters.tasks;

import java.util.List;

import io.quarkiverse.amazon.codegen.model.QuarkusServiceModel;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.emitters.PoetGeneratorTask;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.poet.ClassSpec;

public abstract class BaseGeneratorTasks extends software.amazon.awssdk.codegen.emitters.tasks.BaseGeneratorTasks {
    private final boolean isMain;
    protected final QuarkusServiceModel quarkusModel;

    public BaseGeneratorTasks(GeneratorTaskParams dependencies, QuarkusServiceModel quarkusModel, boolean isMain) {
        super(dependencies);
        this.quarkusModel = quarkusModel;
        this.isMain = isMain;
    }

    public BaseGeneratorTasks(GeneratorTaskParams dependencies, QuarkusServiceModel quarkusModel) {
        this(dependencies, quarkusModel, true);
    }

    /**
     * Add a task generating the class, unless a main class with the same name is already provided in the module
     * sources.
     */
    protected void addPoetGeneratorTask(List<GeneratorTask> generatorTasks, ClassSpec classSpec) {
        if (isMain && quarkusModel.isProvidedInSources(classSpec.className())) {
            return;
        }
        generatorTasks.add(createPoetGeneratorTaskWithEmptyHeader(classSpec));
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
