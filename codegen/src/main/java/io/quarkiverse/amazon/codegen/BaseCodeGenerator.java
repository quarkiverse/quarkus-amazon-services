/*
 * Part of this file is derived from the AWS SDK for Java 2.x, which is licensed under the Apache License, Version 2.0.
 * https://github.com/aws/aws-sdk-java-v2/blob/40b8869c5a24ceae5c41e520ab43e7fbfd06187a/codegen/src/main/java/software/amazon/awssdk/codegen/CodeGenerator.java
 */
package io.quarkiverse.amazon.codegen;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ForkJoinTask;

import com.squareup.javapoet.ClassName;

import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.internal.Jackson;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.utils.Logger;

public abstract class BaseCodeGenerator {
    private static final Logger log = Logger.loggerFor(BaseCodeGenerator.class);
    private static final String MODEL_DIR_NAME = "models";

    private final C2jModels models;
    protected final String sourcesDirectory;
    protected final String resourcesDirectory;
    protected final String testsDirectory;

    /**
     * The prefix for the file name that contains the intermediate model.
     */
    private final String fileNamePrefix;

    static {
        // Make sure ClassName is statically initialized before we do anything in parallel.
        // Parallel static initialization of ClassName and TypeName can result in a deadlock:
        // https://github.com/square/javapoet/issues/799
        ClassName.get(Object.class);
    }

    public BaseCodeGenerator(BaseBuilder builder) {
        this.models = builder.models;
        this.sourcesDirectory = builder.sourcesDirectory;
        this.testsDirectory = builder.testsDirectory;
        this.resourcesDirectory = builder.resourcesDirectory != null ? builder.resourcesDirectory
                : builder.sourcesDirectory;
        this.fileNamePrefix = builder.fileNamePrefix;
    }

    public static File getModelDirectory(String outputDirectory) {
        File dir = new File(outputDirectory, MODEL_DIR_NAME);
        Utils.createDirectory(dir);
        return dir;
    }

    /**
     * load ServiceModel. load code gen configuration from individual client. generate intermediate model. generate
     * code.
     */
    public void execute() {
        try {
            IntermediateModel intermediateModel = new IntermediateModelBuilder(models).build();

            if (fileNamePrefix != null) {
                writeIntermediateModel(intermediateModel);
            }
            emitCode(intermediateModel);

        } catch (Exception e) {
            log.error(() -> "Failed to generate code. ", e);
            throw new RuntimeException(
                    "Failed to generate code. Exception message : " + e.getMessage(), e);
        }
    }

    private void writeIntermediateModel(IntermediateModel model) throws IOException {
        File modelDir = getModelDirectory(sourcesDirectory);
        PrintWriter writer = null;
        try {
            File outDir = new File(sourcesDirectory);
            if (!outDir.exists() && !outDir.mkdirs()) {
                throw new RuntimeException("Failed to create " + outDir.getAbsolutePath());
            }

            File outputFile = new File(modelDir, fileNamePrefix + "-intermediate.json");

            if (!outputFile.exists() && !outputFile.createNewFile()) {
                throw new RuntimeException("Error creating file " + outputFile.getAbsolutePath());
            }

            writer = new PrintWriter(outputFile, "UTF-8");
            Jackson.writeWithObjectMapper(model, writer);
        } finally {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        }
    }

    private void emitCode(IntermediateModel intermediateModel) {
        ForkJoinTask.invokeAll(createGeneratorTasks(intermediateModel));
    }

    abstract GeneratorTask createGeneratorTasks(IntermediateModel intermediateModel);

    /**
     * Builder for a {@link CodeGenerator}.
     */
    public static abstract class BaseBuilder {

        private C2jModels models;
        private String sourcesDirectory;
        private String resourcesDirectory;
        private String testsDirectory;
        private String fileNamePrefix;

        protected BaseBuilder() {
        }

        public BaseBuilder models(C2jModels models) {
            this.models = models;
            return this;
        }

        public BaseBuilder sourcesDirectory(String sourcesDirectory) {
            this.sourcesDirectory = sourcesDirectory;
            return this;
        }

        public BaseBuilder resourcesDirectory(String resourcesDirectory) {
            this.resourcesDirectory = resourcesDirectory;
            return this;
        }

        public BaseBuilder testsDirectory(String smokeTestsDirectory) {
            this.testsDirectory = smokeTestsDirectory;
            return this;
        }

        public BaseBuilder intermediateModelFileNamePrefix(String fileNamePrefix) {
            this.fileNamePrefix = fileNamePrefix;
            return this;
        }

        /**
         * @return An immutable {@link CodeGenerator} object.
         */
        public abstract BaseCodeGenerator build();
    }
}
