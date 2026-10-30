package io.quarkiverse.amazon.sdk.runtime;

import java.util.Map;

import io.quarkiverse.amazon.common.runtime.AsyncHttpClientConfig;
import io.quarkiverse.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.aws")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface AmazonSdkConfig extends GenericClientConfig {
    /** Client overrides, independent of the AWS service. The root holds shared defaults. */
    @ConfigDocMapKey("client-name")
    @WithParentName
    @WithDefaults
    Map<String, GenericClientConfig> clients();

    /** Shared synchronous HTTP transport settings. */
    SyncHttpClientConfig syncClient();

    /** Shared asynchronous HTTP transport settings. */
    AsyncHttpClientConfig asyncClient();
}
