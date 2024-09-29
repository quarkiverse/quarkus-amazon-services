package io.quarkiverse.amazon.common.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.aws.devservices.moto")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface MotoDevServicesBuildTimeConfig {
    /**
     * The Moto container image to use.
     */
    @WithDefault(value = "motoserver/moto")
    String imageName();

    /**
     * Generic properties that are pass for additional container configuration.
     */
    Map<String, String> containerProperties();
}
