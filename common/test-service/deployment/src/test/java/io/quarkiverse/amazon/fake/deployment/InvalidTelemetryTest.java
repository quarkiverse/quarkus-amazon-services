package io.quarkiverse.amazon.fake.deployment;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.fake.FakeClient;

/**
 * Verifies shared telemetry validation reports the dedicated service's config key when its required extension is missing.
 */
class InvalidTelemetryTest {
    @RegisterExtension
    static final QuarkusExtensionTest app = new QuarkusExtensionTest()
            .setForcedDependencies(SdkHttpClients.of(SdkHttpClients.URL_CONNECTION))
            .withApplicationRoot(jar -> jar.addClass(InvalidTelemetryTest.class))
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.fake-service.telemetry.enabled", "true")
            .assertException(error -> {
                Throwable cause = error;
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                }
                assertTrue(cause.getMessage().contains("quarkus.fake-service.telemetry.enabled"), cause.getMessage());
            });

    @Inject
    FakeClient client;

    @Test
    void reportsTheServiceTelemetryKeyWhenItsExtensionIsMissing() {
    }
}
