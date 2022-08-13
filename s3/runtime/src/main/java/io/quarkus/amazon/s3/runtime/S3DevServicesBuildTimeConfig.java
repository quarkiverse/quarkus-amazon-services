package io.quarkus.amazon.s3.runtime;

import java.util.Set;

import io.quarkus.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class S3DevServicesBuildTimeConfig extends DevServicesBuildTimeConfig {

    /**
     * The buckets to create on startup.
     */
    @ConfigItem(defaultValue = "default")
    public Set<String> buckets;
}
