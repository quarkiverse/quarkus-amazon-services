package io.quarkus.amazon.common.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.aws.devservices.localstack")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface LocalStackDevServicesBuildTimeConfig {
    /**
     * The LocalStack container image to use.
     */
    @WithDefault(value = "localstack/localstack:3.0.1")
    String imageName();

    /**
     * Generic properties that are pass for additional container configuration.
     */
    Map<String, String> containerProperties();

    /**
     * Path to init scripts folder executed during localstack startup.
     */
    Optional<String> initScriptsFolder();

    /**
     * Classpath to init scripts folder executed during localstack startup. initScriptsFolder has higher precedence.
     */
    Optional<String> initScriptsClasspath();

    /**
     * Specific container log message to be waiting for localstack init scripts
     * completion.
     */
    Optional<String> initCompletionMsg();

    /**
     * Additional services to be started. Use this property if the service
     * you want is not covered by the extension
     */
    Map<String, DevServicesBuildTimeConfig> additionalServices();

    /**
     * Optional fixed port localstack will listen to.
     */
    Optional<Integer> port();
}
