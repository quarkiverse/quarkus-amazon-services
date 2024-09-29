package io.quarkiverse.amazon.lambda.runtime;

import io.quarkiverse.amazon.common.runtime.AsyncHttpClientBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.HasSdkBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.SyncHttpClientBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/** Amazon LAMBDA build time configuration */
@ConfigMapping(prefix = "quarkus.lambda")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface LambdaBuildTimeConfig extends HasSdkBuildTimeConfig {

    /** Sync HTTP transport configuration for Amazon LAMBDA client */
    SyncHttpClientBuildTimeConfig syncClient();

    /** Async HTTP transport configuration for Amazon LAMBDA client */
    AsyncHttpClientBuildTimeConfig asyncClient();

    /**
     * Config for dev services
     */
    LambdaDevServicesBuildTimeConfig devservices();
}
