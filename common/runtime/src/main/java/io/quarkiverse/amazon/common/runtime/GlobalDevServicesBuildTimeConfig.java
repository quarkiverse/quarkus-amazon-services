package io.quarkiverse.amazon.common.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Global AWS DevServices configuration for selecting the dev services stack.
 * Allows switching between different stack providers like LocalStack or MiniStack.
 */
@ConfigMapping(prefix = "quarkus.aws.devservices")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface GlobalDevServicesBuildTimeConfig {

    /**
     * The dev services stack provider to use.
     * Supported values: "localstack" (default), "ministack", "moto", "floci"
     * <p>
     * This is a global setting that can be overridden per-service using the service-specific
     * devservices.provider configuration.
     * </p>
     */
    @WithDefault(value = "localstack")
    AwsStack provider();

    enum AwsStack {
        LOCALSTACK,
        MINISTACK,
        MOTO,
        FLOCI
    }
}
