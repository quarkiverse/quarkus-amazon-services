package io.quarkiverse.amazon.common.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.aws.devservices.floci")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface FlociDevServicesBuildTimeConfig extends AwsStackDevServicesBuildTimeConfig {
    /**
     * The Floci container image to use.
     * Defaults to {@code floci/floci:latest-compat} when init scripts are configured, otherwise {@code floci/floci:latest}.
     * An explicitly configured image always takes precedence.
     */
    @ConfigDocDefault("floci/floci:latest")
    @Override
    Optional<String> imageName();

    @Override
    default String resolvedImageName() {
        return imageName().orElseGet(() -> initScriptsFolder().isPresent() || initScriptsClasspath().isPresent()
                ? "floci/floci:latest-compat"
                : "floci/floci:latest");
    }

    /**
     * The value of the {@code quarkus-dev-service-floci} label attached to the started container.
     * This property is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services for Floci looks for a container with the
     * {@code quarkus-dev-service-floci} label
     * set to the configured value. If found, it will use this container instead of starting a new one. Otherwise, it
     * starts a new container with the {@code quarkus-dev-service-floci} label set to the specified value.
     * <p>
     * This property is used when you need multiple shared Floci containers.
     */
    @WithDefault("floci")
    @Override
    String serviceName();
}
