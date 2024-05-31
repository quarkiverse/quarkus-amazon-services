package io.quarkiverse.amazon.common.runtime;

import software.amazon.awssdk.http.TlsTrustManagersProvider;

public enum TlsTrustManagersProviderType {
    TRUST_ALL {
        @Override
        public TlsTrustManagersProvider create(TlsTrustManagersProviderConfig config) {
            return new NoneTlsTrustManagersProvider();
        }
    },
    SYSTEM_PROPERTY {
        @Override
        public TlsTrustManagersProvider create(TlsTrustManagersProviderConfig config) {
            // we use the default trust managers, no need to add another one
            return null;
        }
    },
    FILE_STORE {
        @Override
        public TlsTrustManagersProvider create(TlsTrustManagersProviderConfig config) {
            return new FileStoreTlsTrustManagersProvider(config.fileStore());
        }
    };

    public abstract TlsTrustManagersProvider create(TlsTrustManagersProviderConfig config);
}
