package io.quarkus.amazon.common.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface SyncHttpClientBuildTimeConfig {

    /**
     * Type of the sync HTTP client implementation
     */
    @WithDefault(value = "url")
    SyncClientType type();

    public enum SyncClientType {
        URL,
        APACHE,
        AWS_CRT
    }
}
