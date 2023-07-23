package io.quarkus.amazon.sts.runtime;

import io.quarkus.amazon.common.runtime.AsyncHttpClientConfig;
import io.quarkus.amazon.common.runtime.AwsConfig;
import io.quarkus.amazon.common.runtime.SdkConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.sts")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface StsConfig {
    /**
     * AWS SDK client configurations
     */
    @WithParentName
    @ConfigDocSection
    SdkConfig sdk();

    /**
     * AWS services configurations
     */
    @ConfigDocSection
    AwsConfig aws();

    /**
     * Sync HTTP transport configurations
     */
    @ConfigDocSection
    SyncHttpClientConfig syncClient();

    /**
     * Async HTTP transport configurations
     */
    @ConfigDocSection
    AsyncHttpClientConfig asyncClient();
}
