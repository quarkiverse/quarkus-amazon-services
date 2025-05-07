/*
 * Part of this file is derived from the AWS SDK for Java 2.x, which is licensed under the Apache License, Version 2.0.
 * https://github.com/aws/aws-sdk-java-v2/blob/40b8869c5a24ceae5c41e520ab43e7fbfd06187a/codegen-maven-plugin/src/main/java/software/amazon/awssdk/codegen/maven/plugin/GenerationMojo.java
 */
package io.quarkiverse.amazon.codegen.maven.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import io.quarkiverse.amazon.codegen.BaseCodeGenerator;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

public abstract class BaseGenerationMojo extends AbstractMojo {

    private static final String MODEL_FILE = "service-2.json";
    private static final String CUSTOMIZATION_CONFIG_FILE = "customization.config";

    @Parameter(property = "remoteCodegenResourcesRelativePath", defaultValue = "")
    private String remoteCodegenResourcesRelativePath;

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}")
    private String outputDirectory;

    @Parameter(property = "writeIntermediateModel", defaultValue = "false")
    private boolean writeIntermediateModel;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    private Path sourcesDirectory;
    private Path resourcesDirectory;
    private Path testsDirectory;

    private Path codeGenResourcesPath;

    @Override
    public void execute() throws MojoExecutionException {
        this.sourcesDirectory = Paths.get(outputDirectory).resolve("generated-sources").resolve("sdk");
        this.resourcesDirectory = Paths.get(outputDirectory).resolve("generated-resources").resolve("sdk-resources");
        this.testsDirectory = Paths.get(outputDirectory).resolve("generated-test-sources").resolve("sdk-tests");
        this.codeGenResourcesPath = Paths.get(outputDirectory).resolve("codegen-resources");

        downloadModelIfNotExists(project);

        findModelRoots().forEach(p -> {
            Path modelRootPath = p.modelRoot;
            getLog().info("Loading from: " + modelRootPath.toString());
            generateCode(C2jModels.builder()
                    .customizationConfig(p.customizationConfig)
                    .serviceModel(loadServiceModel(modelRootPath))
                    .build());
        });
        project.addCompileSourceRoot(sourcesDirectory.toFile().getAbsolutePath());
        project.addTestCompileSourceRoot(testsDirectory.toFile().getAbsolutePath());
    }

    /**
     * Download the model if it does not exist.
     * Both MODEL_FILE and CUSTOMIZATION_CONFIG_FILE are required.
     * The url to fetch the model from is defined as follow:
     * https://raw.githubusercontent.com/aws/aws-sdk-java-v2/refs/tags/<version>/services/<service>/src/main/resources/codegen-resources/<file>
     * where:
     * version is the version of the SDK (maven project property awssdk.version).
     * file is the name of the file to fetch.
     * service is the name of the service it is the name of the parent folder of the project
     * The file is downloaded to the codeGenResourcesPath if the file doesn't exist yet.
     * If the file cannot be downloaded, a MojoExecutionException is thrown.
     *
     * @throws MojoExecutionException
     */
    private void downloadModelIfNotExists(MavenProject project) throws MojoExecutionException {

        Utils.createDirectory(codeGenResourcesPath.toAbsolutePath().toString());

        String version = project.getProperties().getProperty("awssdk.version");
        String service = project.getParent().getBasedir().getName();

        String baseUrl = "https://raw.githubusercontent.com/aws/aws-sdk-java-v2/refs/tags/" + version + "/services/"
                + service + "/src/main/resources/codegen-resources/";
        if (this.remoteCodegenResourcesRelativePath != null && !this.remoteCodegenResourcesRelativePath.isEmpty()) {
            baseUrl = baseUrl + this.remoteCodegenResourcesRelativePath + "/";
        }

        String modelFileUrl = baseUrl + MODEL_FILE;
        String customizationConfigFileUrl = baseUrl + CUSTOMIZATION_CONFIG_FILE;

        downloadFileIfNotExists(modelFileUrl, codeGenResourcesPath.resolve(MODEL_FILE).toFile());
        downloadFileIfNotExists(customizationConfigFileUrl, codeGenResourcesPath.resolve(CUSTOMIZATION_CONFIG_FILE).toFile());
    }

    private void downloadFileIfNotExists(String url, File destination) throws MojoExecutionException {
        if (!destination.exists()) {
            try {
                downloadFile(url, destination);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to download file from " + url, e);
            }
        }
    }

    private void downloadFile(String url, File destination) throws IOException {

        try (
                ReadableByteChannel readableByteChannel = Channels.newChannel(URI.create(url).toURL().openStream());
                FileOutputStream fileOutputStream = new FileOutputStream(destination)) {
            FileChannel fileChannel = fileOutputStream.getChannel();
            fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        }
    }

    private Stream<ModelRoot> findModelRoots() throws MojoExecutionException {
        try {
            return Files.find(codeGenResourcesPath, 10, this::isModelFile)
                    .map(Path::getParent)
                    .map(p -> new ModelRoot(p, loadCustomizationConfig(p)))
                    .sorted(this::modelSharersLast);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to find '" + MODEL_FILE + "' files in " + codeGenResourcesPath, e);
        }
    }

    private int modelSharersLast(ModelRoot lhs, ModelRoot rhs) {
        return lhs.customizationConfig.getShareModelConfig() == null ? -1 : 1;
    }

    private boolean isModelFile(Path p, BasicFileAttributes a) {
        return p.toString().endsWith(MODEL_FILE);
    }

    private void generateCode(C2jModels models) {
        getCodeGeneratorBuilder(project)
                .models(models)
                .sourcesDirectory(sourcesDirectory.toFile().getAbsolutePath())
                .resourcesDirectory(resourcesDirectory.toFile().getAbsolutePath())
                .testsDirectory(testsDirectory.toFile().getAbsolutePath())
                .intermediateModelFileNamePrefix(intermediateModelFileNamePrefix(models))
                .build()
                .execute();
    }

    abstract BaseCodeGenerator.BaseBuilder getCodeGeneratorBuilder(MavenProject project);

    private String intermediateModelFileNamePrefix(C2jModels models) {
        return writeIntermediateModel ? Utils.getFileNamePrefix(models.serviceModel()) : null;
    }

    private CustomizationConfig loadCustomizationConfig(Path root) {
        return ModelLoaderUtils.loadOptionalModel(CustomizationConfig.class,
                root.resolve(CUSTOMIZATION_CONFIG_FILE).toFile(),
                true)
                .orElse(CustomizationConfig.create());
    }

    private ServiceModel loadServiceModel(Path root) {
        return loadRequiredModel(ServiceModel.class, root.resolve(MODEL_FILE));
    }

    /**
     * Load required model from the project resources.
     */
    private <T> T loadRequiredModel(Class<T> clzz, Path location) {
        return ModelLoaderUtils.loadModel(clzz, location.toFile());
    }

    private static class ModelRoot {
        private final Path modelRoot;
        private final CustomizationConfig customizationConfig;

        private ModelRoot(Path modelRoot, CustomizationConfig customizationConfig) {
            this.modelRoot = modelRoot;
            this.customizationConfig = customizationConfig;
        }
    }
}
