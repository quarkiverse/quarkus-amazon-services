package io.quarkiverse.amazon.s3.runtime;

import java.util.Set;

import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface S3DevServicesBuildTimeConfig extends DevServicesBuildTimeConfig {

    /**
     * The buckets to create on startup.
     */
    @WithDefault(value = "default")
    public Set<String> buckets();
}
