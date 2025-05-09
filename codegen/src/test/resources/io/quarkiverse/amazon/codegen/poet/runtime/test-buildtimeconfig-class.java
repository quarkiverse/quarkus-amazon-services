package io.quarkiverse.amazon.ecr.runtime;

import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.HasSdkBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.HasTransportBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import software.amazon.awssdk.annotations.Generated;

/**
 * Amazon ECR build time configuration
 */
@Generated("io.quarkiverse.amazon:codegen")
@ConfigMapping(prefix = "quarkus.ecr")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface EcrBuildTimeConfig extends HasSdkBuildTimeConfig, HasTransportBuildTimeConfig {

    /**
     * Config for dev services
     */
    DevServicesBuildTimeConfig devservices();
}
