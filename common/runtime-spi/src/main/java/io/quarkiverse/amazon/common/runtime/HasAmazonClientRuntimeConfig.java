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

    /** Defaults used when a named client's optional settings are absent. */
    default AmazonClientConfig defaultsFor(String clientName) {
        return clients().get(ClientUtil.DEFAULT_CLIENT_NAME);
    }

    /** Configuration prefix used in diagnostics, independently of the service identifier. */
    default String configPrefix(String extension, String clientName) {
        return ClientUtil.isDefaultClient(clientName) ? extension : extension + ".\"" + clientName + '"';
    }
}
