package io.quarkiverse.amazon.fake.deployment;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.fake.FakeClient;

/**
 * Verifies shared TLS file-store validation reports the dedicated service's sync transport config key.
 */
class InvalidSyncTransportTest {
    @RegisterExtension
    static final QuarkusExtensionTest app = new QuarkusExtensionTest()
            .setForcedDependencies(SdkHttpClients.of(SdkHttpClients.URL_CONNECTION))
            .withApplicationRoot(jar -> jar.addClass(InvalidSyncTransportTest.class))
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.fake-service.sync-client.tls-key-managers-provider.type", "file-store")
            .assertException(error -> {
                Throwable cause = error;
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                }
                assertTrue(cause.getMessage().contains("quarkus.fake-service.sync-client.tls-key-managers-provider.file-store"),
                        cause.getMessage());
            });

    @Inject
    FakeClient client;

    @Test
    void reportsTheServiceTransportKey() {
    }
}
