package io.quarkus.amazon.common.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "aws.devservices.localstack", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class LocalStackDevServicesBuildTimeConfig {
    /**
     * The LocalStack container image to use.
     */
    @ConfigItem(defaultValue = "localstack/localstack:1.4.0")
    public String imageName;

    /**
     * Generic properties that are pass for additional container configuration.
     */
    @ConfigItem
    public Map<String, String> containerProperties;

    /**
     * Path to init scripts folder executed during localstack startup.
     */
    @ConfigItem
    public Optional<String> initScriptsFolder;

    /**
     * Specific container log message to be waiting for localstack init scripts
     * completion.
     */
    @ConfigItem
    public Optional<String> initCompletionMsg;

    /**
     * Additional services to be started. Use this property if the service
     * you want is not covered by the extension
     */
    @ConfigItem
    public Map<String, DevServicesBuildTimeConfig> additionalServices;
}
