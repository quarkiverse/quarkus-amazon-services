package io.quarkus.amazon.common.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "aws.devservices.moto", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class MotoDevServicesBuildTimeConfig {
    /**
     * The localstack container image to use.
     */
    @ConfigItem(defaultValue = "motoserver/moto")
    public String imageName;

    /**
     * Generic properties that are pass for additional container configuration.
     */
    @ConfigItem
    public Map<String, String> containerProperties;
}
