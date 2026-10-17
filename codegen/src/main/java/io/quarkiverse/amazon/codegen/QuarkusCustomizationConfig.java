package io.quarkiverse.amazon.codegen;

/**
 * Quarkus specific customizations of the generated extension, loaded from the optional
 * {@code quarkus-customization.config} file located next to the service model.
 * <p>
 * Each property is optional and defaults to the value derived from the service model.
 */
public class QuarkusCustomizationConfig {

    /**
     * The last segment of the extension package {@code io.quarkiverse.amazon.<packageName>}.
     * Defaults to the SDK client package name.
     */
    private String packageName;

    /**
     * The prefix of the generated classes (e.g. {@code <classPrefix>Config}).
     * Defaults to the SDK service name.
     */
    private String classPrefix;

    /**
     * The configuration root name {@code quarkus.<configName>}.
     * Defaults to the SDK client package name.
     */
    private String configName;

    /**
     * The Quarkus feature name.
     * Defaults to {@code amazon-sdk-<client package name>}.
     */
    private String featureName;

    /**
     * The name of the service folder in the AWS SDK repository to download the model from.
     * Defaults to the name of the extension parent module folder.
     */
    private String sdkServiceName;

    /**
     * The name of the service displayed in the documentation.
     * Defaults to the SDK descriptive service name (the service abbreviation, or the service full name).
     */
    private String displayName;

    /**
     * The name of the documentation page, without extension.
     * Defaults to {@code amazon-<packageName>}.
     */
    private String docsPageName;

    public static QuarkusCustomizationConfig create() {
        return new QuarkusCustomizationConfig();
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getClassPrefix() {
        return classPrefix;
    }

    public void setClassPrefix(String classPrefix) {
        this.classPrefix = classPrefix;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public String getFeatureName() {
        return featureName;
    }

    public void setFeatureName(String featureName) {
        this.featureName = featureName;
    }

    public String getSdkServiceName() {
        return sdkServiceName;
    }

    public void setSdkServiceName(String sdkServiceName) {
        this.sdkServiceName = sdkServiceName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDocsPageName() {
        return docsPageName;
    }

    public void setDocsPageName(String docsPageName) {
        this.docsPageName = docsPageName;
    }
}
