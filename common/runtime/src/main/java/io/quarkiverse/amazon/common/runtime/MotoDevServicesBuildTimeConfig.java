package io.quarkiverse.amazon.common.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.aws.devservices.moto")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface MotoDevServicesBuildTimeConfig extends AwsStackDevServicesBuildTimeConfig {
    /**
     * The Moto container image to use.
     */
    @WithDefault(value = "motoserver/moto:latest")
    @Override
    String imageName();

    /**
     * The value of the {@code quarkus-dev-service-moto} label attached to the started container.
     * This property is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services for Moto looks for a container with the
     * {@code quarkus-dev-service-moto} label
     * set to the configured value. If found, it will use this container instead of starting a new one. Otherwise, it
     * starts a new container with the {@code quarkus-dev-service-moto} label set to the specified value.
     * <p>
     * This property is used when you need multiple shared Moto containers.
     */
    @WithDefault("moto")
    @Override
    String serviceName();
}