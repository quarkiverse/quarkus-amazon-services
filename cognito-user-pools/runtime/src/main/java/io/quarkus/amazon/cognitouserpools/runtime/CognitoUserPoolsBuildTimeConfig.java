package io.quarkus.amazon.cognitouserpools.runtime;

import io.quarkus.amazon.common.runtime.AsyncHttpClientBuildTimeConfig;
import io.quarkus.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkus.amazon.common.runtime.SdkBuildTimeConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Amazon Cognito Identity Provider (User Pools) build time configuration
 */
@ConfigRoot(name = "cognito-user-pools", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class CognitoUserPoolsBuildTimeConfig {

    /**
     * SDK client configurations for AWS Cognito Identity Provider client
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public SdkBuildTimeConfig sdk;

    /**
     * Sync HTTP transport configuration for Amazon Cognito Identity Provider client
     */
    @ConfigItem
    public SyncHttpClientBuildTimeConfig syncClient;

    /**
     * Async HTTP transport configuration for Amazon Cognito Identity Provider client
     */
    @ConfigItem
    public AsyncHttpClientBuildTimeConfig asyncClient;

    /**
     * Config for dev services
     */
    @ConfigItem
    public DevServicesBuildTimeConfig devservices;
}
