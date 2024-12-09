package io.quarkiverse.amazon.paymentcryptography.runtime;

import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.HasSdkBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.HasTransportBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * Amazon Payment Cryptography build time configuration
 */
@ConfigMapping(prefix = "quarkus.paymentcryptography")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface PaymentCryptographyBuildTimeConfig extends HasSdkBuildTimeConfig, HasTransportBuildTimeConfig {

    /**
     * Config for dev services
     */
    DevServicesBuildTimeConfig devservices();
}
