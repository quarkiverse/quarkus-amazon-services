package io.quarkiverse.amazon.fake.deployment;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.fake.FakeAsyncClient;

/** Verifies shared Netty concurrency validation; the generic ConfigValidationPrefixTest checks its alternate prefix. */
class InvalidAsyncTransportTest {
    @RegisterExtension
    static final QuarkusExtensionTest app = new QuarkusExtensionTest()
            .setForcedDependencies(SdkHttpClients.of(SdkHttpClients.NETTY))
            .withApplicationRoot(jar -> jar.addClass(InvalidAsyncTransportTest.class))
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.fake-service.async-client.max-concurrency", "0")
            .assertException(error -> {
                Throwable cause = error;
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                }
                assertTrue(cause.getMessage().contains("quarkus.fake-service.async-client.max-concurrency"),
                        cause.getMessage());
            });

    @Inject
    FakeAsyncClient client;

    @Test
    void reportsTheServiceTransportKey() {
    }
}
