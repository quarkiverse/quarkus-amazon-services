package io.quarkiverse.amazon.sesv2.runtime;

import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.HasSdkBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.HasTransportBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * Amazon SESv2 build time configuration
 */
@ConfigMapping(prefix = "quarkus.sesv2")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface SesV2BuildTimeConfig extends HasSdkBuildTimeConfig, HasTransportBuildTimeConfig {

    /**
     * Config for dev services
     */
    DevServicesBuildTimeConfig devservices();
}
