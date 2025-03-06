package io.quarkiverse.amazon.common.runtime;

public interface HasTransportBuildTimeConfig {
    /**
     * Sync HTTP transport configuration
     */
    SyncHttpClientBuildTimeConfig syncClient();

    /**
     * Async HTTP transport configuration
     */
    AsyncHttpClientBuildTimeConfig asyncClient();
}
