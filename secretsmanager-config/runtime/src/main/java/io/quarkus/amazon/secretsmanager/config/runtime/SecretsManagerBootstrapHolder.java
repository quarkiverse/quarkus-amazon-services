package io.quarkus.amazon.secretsmanager.config.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.amazon.common.runtime.AbstractAmazonClientTransportRecorder;

final class SecretsManagerBootstrapHolder {

    static AbstractAmazonClientTransportRecorder transportRecorder;
    static Optional<List<String>> interceptors;

    static Optional<List<String>> getInterceptors() {
        return interceptors;
    }

    static AbstractAmazonClientTransportRecorder getTransportRecorder() {
        return transportRecorder;
    }

    static void setup(AbstractAmazonClientTransportRecorder transportRecorder, Optional<List<String>> interceptors) {
        SecretsManagerBootstrapHolder.transportRecorder = transportRecorder;
        SecretsManagerBootstrapHolder.interceptors = interceptors;
    }
}
