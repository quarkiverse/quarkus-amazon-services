package io.quarkiverse.amazon.common.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.aws.devservices.ministack")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface MiniStackDevServicesBuildTimeConfig extends AwsStackDevServicesBuildTimeConfig {
    /**
     * The MiniStack container image to use.
     */
    @WithDefault(value = "ministackorg/ministack:latest")
    @Override
    String imageName();

    /**
     * The value of the {@code quarkus-dev-service-ministack} label attached to the started container.
     * This property is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services for MiniStack looks for a container with the
     * {@code quarkus-dev-service-ministack} label
     * set to the configured value. If found, it will use this container instead of starting a new one. Otherwise, it
     * starts a new container with the {@code quarkus-dev-service-ministack} label set to the specified value.
     * <p>
     * This property is used when you need multiple shared MiniStack containers.
     */
    @WithDefault("ministack")
    @Override
    String serviceName();
}
