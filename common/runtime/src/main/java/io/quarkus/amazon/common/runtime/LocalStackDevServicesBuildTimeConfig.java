package io.quarkus.amazon.common.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "aws.devservices.localstack", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class LocalStackDevServicesBuildTimeConfig {
    /**
     * If local stack based dev services should be used. This requires explicit
     * config rather than
     * being activated by a lack of config for dev services, as AWS is a singleton
     * and does not seen
     * to be configured in the same way as other services.
     *
     * If this is true then a localstack container will be started and will be
     * used instead of AWS.
     */
    @ConfigItem
    public boolean enabled;

    /**
     * The localstack container image to use.
     */
    @ConfigItem(defaultValue = "localstack/localstack")
    public String imageName;

    /**
     * Generic properties that are pass for additional container configuration.
     */
    @ConfigItem
    public Map<String, String> containerProperties;
}
