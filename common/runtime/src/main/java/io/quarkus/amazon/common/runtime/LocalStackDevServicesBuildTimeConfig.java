package io.quarkus.amazon.common.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "aws.devservices.localstack", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class LocalStackDevServicesBuildTimeConfig {
    /**
     * The LocalStack container image to use.
     */
    @ConfigItem(defaultValue = "localstack/localstack")
    public String imageName;

    /**
     * Generic properties that are pass for additional container configuration.
     */
    @ConfigItem
    public Map<String, String> containerProperties;
}
