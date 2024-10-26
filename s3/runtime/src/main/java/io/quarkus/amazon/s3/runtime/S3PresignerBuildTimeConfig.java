package io.quarkus.amazon.s3.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Amazon S3 build time configuration
 */
@ConfigMapping(prefix = "quarkus.s3")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface S3PresignerBuildTimeConfig {

    /**
     * When `true`, a `S3Presigner` default bean is produced even if no injection points are discovered.
     *
     * Note: The default value ensures backward compatibility; you can disable it. It will be removed in the next major version.
     */
    @WithDefault(value = "true")
    boolean unremovableS3PresignerBean();
}
