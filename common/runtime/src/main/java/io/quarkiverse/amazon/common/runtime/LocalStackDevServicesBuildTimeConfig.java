package io.quarkiverse.amazon.common.runtime;

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

}
