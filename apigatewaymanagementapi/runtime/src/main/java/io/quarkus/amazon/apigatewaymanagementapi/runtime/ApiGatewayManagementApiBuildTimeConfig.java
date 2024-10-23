package io.quarkus.amazon.apigatewaymanagementapi.runtime;

import io.quarkus.amazon.common.runtime.AsyncHttpClientBuildTimeConfig;
import io.quarkus.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkus.amazon.common.runtime.HasSdkBuildTimeConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * Amazon Inspector build time configuration
 */
@ConfigMapping(prefix = "quarkus.apigatewaymanagementapi")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface ApiGatewayManagementApiBuildTimeConfig extends HasSdkBuildTimeConfig {

    /**
     * Sync HTTP transport configuration for Amazon Inspector client
     */
    SyncHttpClientBuildTimeConfig syncClient();

    /**
     * Async HTTP transport configuration for Amazon Inspector client
     */
    AsyncHttpClientBuildTimeConfig asyncClient();

    /**
     * Config for dev services
     */
    DevServicesBuildTimeConfig devservices();
}
