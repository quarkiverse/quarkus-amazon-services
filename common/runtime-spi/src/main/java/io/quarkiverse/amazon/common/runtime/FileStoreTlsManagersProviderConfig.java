package io.quarkiverse.amazon.common.runtime;

import java.nio.file.Path;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface FileStoreTlsManagersProviderConfig {

    /**
     * Path to the key store.
     */
    Optional<Path> path();

    /**
     * Key store type.
     * <p>
     * See the KeyStore section in
     * the https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#KeyStore[Java Cryptography
     * Architecture Standard Algorithm Name Documentation]
     * for information about standard keystore types.
     */
    Optional<String> type();

    /**
     * Key store password
     */
    Optional<String> password();
}
