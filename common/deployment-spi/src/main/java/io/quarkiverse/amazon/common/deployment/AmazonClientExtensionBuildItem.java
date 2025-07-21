package io.quarkiverse.amazon.common.deployment;

import org.jboss.jandex.DotName;

import io.quarkiverse.amazon.common.runtime.AmazonClientRecorder;
import io.quarkiverse.amazon.common.runtime.AsyncHttpClientBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.AsyncHttpClientConfig;
import io.quarkiverse.amazon.common.runtime.HasAmazonClientRuntimeConfig;
import io.quarkiverse.amazon.common.runtime.HasSdkBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.HasTransportBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.SyncHttpClientBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * Represents a declared Amazon client extension with its configuration and builder classes.
 */
public final class AmazonClientExtensionBuildItem extends MultiBuildItem {

    private final String configName;
    private final HasTransportBuildTimeConfig transportBuildTimeConfig;
    private final String amazonServiceClientName;
    private final String builtinInterceptorsPath;
    private final DotName syncClientName;
    private final DotName asyncClientName;
    private final DotName presignerClientName;
    private final HasSdkBuildTimeConfig hasSdkBuildTimeConfig;
    private final Class<?> syncClientBuilderClass;
    private final Class<?> asyncClientBuilderClass;
    private final Class<?> presignerBuilderClass;
    private final RuntimeValue<SyncHttpClientConfig> syncConfig;
    private final RuntimeValue<AsyncHttpClientConfig> asyncConfig;
    private final RuntimeValue<? extends HasAmazonClientRuntimeConfig> amazonClientsConfig;

    public AmazonClientExtensionBuildItem(
            String configName,
            String amazonServiceClientName,
            String builtinInterceptorsPath,
            DotName syncClientName,
            Class<?> syncClientBuilderClass,
            DotName asyncClientName,
            Class<?> asyncClientBuilderClass,
            DotName presignerClientName,
            Class<?> presignerBuilderClass,
            AmazonClientRecorder recorder,
            HasTransportBuildTimeConfig transportBuildTimeConfig,
            HasSdkBuildTimeConfig hasSdkBuildTimeConfig) {
        this.configName = configName;
        this.amazonServiceClientName = amazonServiceClientName;
        this.builtinInterceptorsPath = builtinInterceptorsPath;
        this.syncClientName = syncClientName;
        this.syncClientBuilderClass = syncClientBuilderClass;
        this.asyncClientName = asyncClientName;
        this.asyncClientBuilderClass = asyncClientBuilderClass;
        this.presignerClientName = presignerClientName;
        this.presignerBuilderClass = presignerBuilderClass;
        this.transportBuildTimeConfig = transportBuildTimeConfig;
        this.hasSdkBuildTimeConfig = hasSdkBuildTimeConfig;

        // capture these runtime values so they can be reused in non @Record build step
        this.syncConfig = recorder.getSyncConfig();
        this.asyncConfig = recorder.getAsyncConfig();
        this.amazonClientsConfig = recorder.getAmazonClientsConfig();
    }

    public String getConfigName() {
        return configName;
    }

    public String getAmazonServiceClientName() {
        return amazonServiceClientName;
    }

    public String getBuiltinInterceptorsPath() {
        return builtinInterceptorsPath;
    }

    public DotName getSyncClientName() {
        return syncClientName;
    }

    public DotName getAsyncClientName() {
        return asyncClientName;
    }

    public DotName getPresignerClientName() {
        return presignerClientName;
    }

    public AsyncHttpClientBuildTimeConfig getBuildAsyncConfig() {
        return transportBuildTimeConfig.asyncClient();
    }

    public RuntimeValue<AsyncHttpClientConfig> getAsyncConfig() {
        return asyncConfig;
    }

    public SyncHttpClientBuildTimeConfig getBuildSyncConfig() {
        return transportBuildTimeConfig.syncClient();
    }

    public RuntimeValue<SyncHttpClientConfig> getSyncConfig() {
        return syncConfig;
    }

    public HasSdkBuildTimeConfig getHasSdkBuildTimeConfig() {
        return hasSdkBuildTimeConfig;
    }

    public RuntimeValue<? extends HasAmazonClientRuntimeConfig> getAmazonClientsConfig() {
        return amazonClientsConfig;
    }

    public Class<?> getSyncClientBuilderClass() {
        return syncClientBuilderClass;
    }

    public Class<?> getAsyncClientBuilderClass() {
        return asyncClientBuilderClass;
    }

    public Class<?> getPresignerBuilderClass() {
        return presignerBuilderClass;
    }
}
