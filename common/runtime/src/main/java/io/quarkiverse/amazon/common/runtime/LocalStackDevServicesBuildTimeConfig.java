package io.quarkiverse.amazon.common.runtime;

import java.util.function.BooleanSupplier;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.aws.devservices.localstack")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface LocalStackDevServicesBuildTimeConfig extends AwsStackDevServicesBuildTimeConfig {

    /**
     * The value of the {@code quarkus-dev-service-localstack} label attached to the started container.
     */
    @WithDefault("localstack")
    @Override
    String serviceName();

    /**
     * The LocalStack container image to use.
     */
    @WithDefault(value = "localstack/localstack:4.13")
    @Override
    String imageName();

    /**
     * When legacy mode is enabled, Dev Services for LocalStack will use the old approach to manage the container lifecycle.
     * <p>
     * DEPRECATED:
     * This mode is deprecated and should not be used. It is only provided as a fallback for users who rely on the old behavior
     * and need more time to migrate to the new approach.
     * </p>
     */
    @WithDefault("false")
    Boolean legacyMode();

    class LegacyModeEnabled implements BooleanSupplier {

        final LocalStackDevServicesBuildTimeConfig config;

        public LegacyModeEnabled(LocalStackDevServicesBuildTimeConfig config) {
            this.config = config;
        }

        @Override
        public boolean getAsBoolean() {
            return config.legacyMode();
        }
    }
}
