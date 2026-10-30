package io.quarkiverse.amazon.fake.deployment;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.fake.FakeClient;

/**
 * Verifies shared interceptor validation reports the dedicated service's config key.
 */
class InvalidInterceptorsTest {
    @RegisterExtension
    static final QuarkusExtensionTest app = new QuarkusExtensionTest()
            .setForcedDependencies(SdkHttpClients.of(SdkHttpClients.URL_CONNECTION))
            .withApplicationRoot(jar -> jar.addClass(InvalidInterceptorsTest.class))
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.fake-service.interceptors", "no.such.Interceptor")
            .assertException(error -> {
                Throwable cause = error;
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                }
                assertTrue(cause.getMessage().contains("quarkus.fake-service.interceptors"), cause.getMessage());
            });

    @Inject
    FakeClient client;

    @Test
    void reportsTheServiceInterceptorKey() {
    }
}
