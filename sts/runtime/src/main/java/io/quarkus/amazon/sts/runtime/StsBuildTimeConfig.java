package io.quarkus.amazon.sts.runtime;

import io.quarkus.amazon.common.runtime.SdkBuildTimeConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "sts", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class StsBuildTimeConfig {

    /**
     * SDK client configurations for AWS STS client
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public SdkBuildTimeConfig sdk;

    /**
     * Sync HTTP transport configuration for Amazon STS client
     */
    @ConfigItem
    public SyncHttpClientBuildTimeConfig syncClient;
}
