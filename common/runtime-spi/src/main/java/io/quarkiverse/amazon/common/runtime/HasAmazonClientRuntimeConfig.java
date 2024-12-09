package io.quarkiverse.amazon.common.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

public interface HasAmazonClientRuntimeConfig {

    /**
     * Clients
     */
    @ConfigDocMapKey("client-name")
    @WithParentName
    @WithDefaults
    @WithUnnamedKey(ClientUtil.DEFAULT_CLIENT_NAME)
    Map<String, AmazonClientConfig> clients();
}
