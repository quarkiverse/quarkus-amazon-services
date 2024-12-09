package io.quarkiverse.amazon.common.deployment;

import java.util.Optional;

import org.jboss.jandex.DotName;

import io.quarkiverse.amazon.common.runtime.AsyncHttpClientBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.SdkBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.SyncHttpClientBuildTimeConfig;
import io.quarkus.builder.item.MultiBuildItem;

/**
 * Describes what client transport builders are required for a given extension
 */
public final class RequireAmazonClientTransportBuilderBuildItem extends MultiBuildItem {
    private final Optional<DotName> syncClassName;
    private final Optional<DotName> asyncClassName;
    private final String awsClientName;
    private final SdkBuildTimeConfig buildTimeSdkConfig;
    private final SyncHttpClientBuildTimeConfig buildTimeSyncConfig;
    private final AsyncHttpClientBuildTimeConfig buildTimeAsyncConfig;

    public RequireAmazonClientTransportBuilderBuildItem(Optional<DotName> syncClassName, Optional<DotName> asyncClassName,
            String awsClientName, SdkBuildTimeConfig buildTimeSdkConfig,
            SyncHttpClientBuildTimeConfig buildTimeSyncConfig,
            AsyncHttpClientBuildTimeConfig buildTimeAsyncConfig) {
        this.syncClassName = syncClassName;
        this.asyncClassName = asyncClassName;
        this.awsClientName = awsClientName;
        this.buildTimeSdkConfig = buildTimeSdkConfig;
        this.buildTimeSyncConfig = buildTimeSyncConfig;
        this.buildTimeAsyncConfig = buildTimeAsyncConfig;
    }

    public Optional<DotName> getSyncClassName() {
        return syncClassName;
    }

    public Optional<DotName> getAsyncClassName() {
        return asyncClassName;
    }

    public String getAwsClientName() {
        return awsClientName;
    }

    public SdkBuildTimeConfig getBuildTimeSdkConfig() {
        return buildTimeSdkConfig;
    }

    public SyncHttpClientBuildTimeConfig getBuildTimeSyncConfig() {
        return buildTimeSyncConfig;
    }

    public AsyncHttpClientBuildTimeConfig getBuildTimeAsyncConfig() {
        return buildTimeAsyncConfig;
    }
}
