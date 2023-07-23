package io.quarkus.amazon.common.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface AsyncHttpClientBuildTimeConfig {

    /**
     * Type of the async HTTP client implementation
     */
    @WithDefault(value = "netty")
    AsyncClientType type();

    public enum AsyncClientType {
        NETTY,
        AWS_CRT
    }
}
