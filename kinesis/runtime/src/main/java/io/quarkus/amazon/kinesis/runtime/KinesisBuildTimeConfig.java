package io.quarkus.amazon.kinesis.runtime;

import io.quarkus.amazon.common.runtime.AsyncHttpClientBuildTimeConfig;
import io.quarkus.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkus.amazon.common.runtime.HasSdkBuildTimeConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * Amazon Kinesis build time configuration
 */
@ConfigMapping(prefix = "quarkus.kinesis")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface KinesisBuildTimeConfig extends HasSdkBuildTimeConfig {

    /**
     * Sync HTTP transport configuration for Amazon Kinesis client
     */
    SyncHttpClientBuildTimeConfig syncClient();

    /**
     * Async HTTP transport configuration for Amazon Kinesis client
     */
    AsyncHttpClientBuildTimeConfig asyncClient();

    /**
     * Config for dev services
     */
    DevServicesBuildTimeConfig devservices();
}
