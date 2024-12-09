package io.quarkiverse.amazon.s3.runtime;

import io.quarkiverse.amazon.common.runtime.HasSdkBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.HasTransportBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * Amazon S3 build time configuration
 */
@ConfigMapping(prefix = "quarkus.s3")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface S3BuildTimeConfig extends HasSdkBuildTimeConfig, HasTransportBuildTimeConfig {

    /**
     * Config for dev services
     */
    S3DevServicesBuildTimeConfig devservices();
}
