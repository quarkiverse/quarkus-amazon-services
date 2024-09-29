package io.quarkiverse.amazon.common.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface TlsTrustManagersProviderConfig {

    // @formatter:off
    /**
     * TLS trust managers provider type.
     *
     * Available providers:
     *
     * * `trust-all` - Use this provider to disable the validation of servers certificates and therefore trust all server certificates.
     * * `system-property` - Provider checks the standard `javax.net.ssl.keyStore`, `javax.net.ssl.keyStorePassword`, and
     *                       `javax.net.ssl.keyStoreType` properties defined by the
     *                        https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html[JSSE].
     * * `file-store` - Provider that loads the key store from a file.
     *
     * @asciidoclet
     */
    // @formatter:on
    @WithDefault("system-property")
    TlsTrustManagersProviderType type();

    /**
     * Configuration of the file store provider.
     * <p>
     * Used only if {@code FILE_STORE} type is chosen.
     */
    FileStoreTlsManagersProviderConfig fileStore();

}
