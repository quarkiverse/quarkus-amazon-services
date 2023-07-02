package io.quarkus.amazon.common.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class AsyncHttpClientBuildTimeConfig {

    /**
     * Type of the async HTTP client implementation
     */
    @ConfigItem(defaultValue = "netty")
    public AsyncClientType type;

    public enum AsyncClientType {
        NETTY,
        AWS_CRT
    }
}
