package io.quarkiverse.amazon.iam.runtime;

import io.quarkiverse.amazon.common.runtime.AsyncHttpClientBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.HasSdkBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.SyncHttpClientBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * Amazon IAM build time configuration
 */
@ConfigMapping(prefix = "quarkus.iam")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface IamBuildTimeConfig extends HasSdkBuildTimeConfig {

    /**
     * Sync HTTP transport configuration for Amazon IAM client
     */
    SyncHttpClientBuildTimeConfig syncClient();

    /**
     * Async HTTP transport configuration for Amazon IAM client
     */
    AsyncHttpClientBuildTimeConfig asyncClient();

    /**
     * Config for dev services
     */
    DevServicesBuildTimeConfig devservices();
}
