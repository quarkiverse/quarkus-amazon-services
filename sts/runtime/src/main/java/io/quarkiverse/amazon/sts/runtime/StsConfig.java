package io.quarkiverse.amazon.sts.runtime;

import io.quarkiverse.amazon.common.runtime.AsyncHttpClientConfig;
import io.quarkiverse.amazon.common.runtime.HasAmazonClientRuntimeConfig;
import io.quarkiverse.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "quarkus.sts")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface StsConfig extends HasAmazonClientRuntimeConfig {
    /**
     * Sync HTTP transport configurations
     */
    @ConfigDocSection
    SyncHttpClientConfig syncClient();

    /**
     * Async HTTP transport configurations
     */
    @ConfigDocSection
    AsyncHttpClientConfig asyncClient();
}
