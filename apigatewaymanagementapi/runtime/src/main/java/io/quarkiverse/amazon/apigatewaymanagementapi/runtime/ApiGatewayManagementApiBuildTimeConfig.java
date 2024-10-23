package io.quarkiverse.amazon.apigatewaymanagementapi.runtime;

import io.quarkiverse.amazon.common.runtime.AsyncHttpClientBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.HasSdkBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.SyncHttpClientBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * Amazon Api Gateway Management Api build time configuration
 */
@ConfigMapping(prefix = "quarkus.apigatewaymanagementapi")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface ApiGatewayManagementApiBuildTimeConfig extends HasSdkBuildTimeConfig {

    /**
     * Sync HTTP transport configuration for Amazon Api Gateway Management Api client
     */
    SyncHttpClientBuildTimeConfig syncClient();

    /**
     * Async HTTP transport configuration for Amazon Api Gateway Management Api client
     */
    AsyncHttpClientBuildTimeConfig asyncClient();

    /**
     * Config for dev services
     */
    DevServicesBuildTimeConfig devservices();
}
