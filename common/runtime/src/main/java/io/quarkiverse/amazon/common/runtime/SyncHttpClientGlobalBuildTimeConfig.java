package io.quarkiverse.amazon.common.runtime;

import io.quarkus.runtime.annotations.ConfigDocFilename;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Global configuration of the sync HTTP clients.
 */
@ConfigMapping(prefix = "quarkus.aws.sync-client")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigDocFilename("quarkus-amazon-sync-client.adoc")
public interface SyncHttpClientGlobalBuildTimeConfig {

    /**
     * Whether to log a warning at build time when the Apache HTTP client is used.
     * <p>
     * The AWS SDK deprecated the Apache HTTP client ({@code apache-client}) in favor of the Apache 5.x HTTP client
     * ({@code apache5-client}).
     * </p>
     */
    @WithDefault("true")
    boolean apacheDeprecationWarning();
}
