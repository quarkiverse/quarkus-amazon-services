package io.quarkiverse.amazon.codegen.model;

import java.io.File;

import com.squareup.javapoet.ClassName;

import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;

/**
 * Model of the Quarkus extension to generate for a service: the AWS SDK intermediate model, the names of the
 * generated Quarkus artifacts and what is already provided by the module sources.
 * <p>
 * Instances are created with a {@link QuarkusServiceModelBuilder}.
 */
public class QuarkusServiceModel {
    private final IntermediateModel intermediateModel;
    private final String packageName;
    private final String classPrefix;
    private final String configName;
    private final String featureName;
    private final String displayName;
    private final String docsPageName;
    private final String sourcesDirectory;

    QuarkusServiceModel(IntermediateModel intermediateModel, String packageName, String classPrefix, String configName,
            String featureName, String displayName, String docsPageName, String sourcesDirectory) {
        this.intermediateModel = intermediateModel;
        this.packageName = packageName;
        this.classPrefix = classPrefix;
        this.configName = configName;
        this.featureName = featureName;
        this.displayName = displayName;
        this.docsPageName = docsPageName;
        this.sourcesDirectory = sourcesDirectory;
    }

    public IntermediateModel intermediateModel() {
        return intermediateModel;
    }

    public String packageName() {
        return packageName;
    }

    public String basePackage() {
        return "io.quarkiverse.amazon." + packageName;
    }

    public String runtimePackage() {
        return basePackage() + ".runtime";
    }

    public String deploymentPackage() {
        return basePackage() + ".deployment";
    }

    public String testPackage() {
        return basePackage() + ".test";
    }

    public String devServicesPackage() {
        return "io.quarkiverse.amazon.devservices." + packageName;
    }

    public String configName() {
        return configName;
    }

    public String configPrefix() {
        return "quarkus." + configName;
    }

    public String featureName() {
        return featureName;
    }

    /**
     * @return the name of the service displayed in the documentation
     */
    public String displayName() {
        return displayName;
    }

    /**
     * @return the name of the documentation page, without extension
     */
    public String docsPageName() {
        return docsPageName;
    }

    public ClassName runtimeClass(String suffix) {
        return ClassName.get(runtimePackage(), classPrefix + suffix);
    }

    public ClassName deploymentClass(String suffix) {
        return ClassName.get(deploymentPackage(), classPrefix + suffix);
    }

    public ClassName baseClass(String suffix) {
        return ClassName.get(basePackage(), classPrefix + suffix);
    }

    public ClassName devServicesClass(String suffix) {
        return ClassName.get(devServicesPackage(), classPrefix + suffix);
    }

    public String classPrefix() {
        return classPrefix;
    }

    public ClassName testClass(String suffix) {
        return ClassName.get(testPackage(), classPrefix + suffix);
    }

    /**
     * @return true if the class is already provided in the module sources and must not be generated
     */
    public boolean isProvidedInSources(ClassName className) {
        return sourcesDirectory != null
                && new File(sourcesDirectory, Utils.packageToDirectory(className.packageName()) + '/'
                        + className.simpleName() + ".java").exists();
    }

    /**
     * @return true if the module provides its own dev services build time configuration
     */
    public boolean hasCustomDevServicesBuildTimeConfig() {
        return isProvidedInSources(runtimeClass("DevServicesBuildTimeConfig"));
    }
}
