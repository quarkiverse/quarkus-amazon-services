package io.quarkiverse.amazon.sdk.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.ecr.EcrClient;

/**
 * Verifies an endpoint without a scheme for a dotted client name is rejected with its quoted named config key.
 */
class InvalidEndpointForNamedClientTest {
    @RegisterExtension
    static final QuarkusExtensionTest app = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(InvalidEndpointForNamedClientTest.class))
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.aws.region", "eu-west-1")
            .overrideConfigKey("quarkus.aws.credentials.type", "anonymous")
            // The dotted client name is valid; the endpoint is missing its scheme.
            .overrideConfigKey("quarkus.aws.\"archive.eu\".endpoint-override", "localhost")
            .assertException(error -> {
                Throwable cause = error;
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                }
                assertEquals("quarkus.aws.\"archive.eu\".endpoint-override (localhost) - scheme must be specified",
                        cause.getMessage());
            });

    @Inject
    @AmazonClient("archive.eu")
    EcrClient client;

    @Test
    void rejectsMissingEndpointSchemeForAValidDottedClientName() {
    }
}
