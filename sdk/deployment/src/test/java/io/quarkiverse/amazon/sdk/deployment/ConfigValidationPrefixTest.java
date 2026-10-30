package io.quarkiverse.amazon.sdk.deployment;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.ecr.EcrAsyncClient;

/**
 * Verifies the generic prefix for shared validation covered by the historical InvalidAsyncTransportTest.
 */
class ConfigValidationPrefixTest {
    @RegisterExtension
    static final QuarkusExtensionTest app = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(ConfigValidationPrefixTest.class))
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.aws.async-client.max-concurrency", "0")
            .assertException(error -> {
                Throwable cause = error;
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                }
                assertTrue(cause.getMessage().contains("quarkus.aws.async-client.max-concurrency"), cause.getMessage());
            });

    @Inject
    EcrAsyncClient client;

    @Test
    void reportsTheGenericConfigurationPrefixInValidationErrors() {
    }
}
