package io.quarkiverse.amazon.sdk.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.ecr.EcrClient;

/**
 * Verifies disabling generic Dev Services prevents stack requests and endpoint injection for default and named clients.
 * Historical counterpart: {@code io.quarkiverse.amazon.fake.deployment.devservices.DevServicesDisabledTest}.
 */
class DevServicesDisabledTest {
    @RegisterExtension
    static final QuarkusExtensionTest app = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(StubAwsStackProcessor.class)
                    .addAsResource(new StringAsset(StubAwsStackProcessor.class.getName()),
                            "META-INF/quarkus-build-steps.list"))
            // Disable real containers only; the stub still observes the service's provider requests.
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.aws.devservices.enabled", "false")
            .overrideConfigKey("quarkus.aws.region", "us-east-1")
            .overrideConfigKey("quarkus.aws.credentials.type", "anonymous");

    @Inject
    EcrClient defaultClient;

    @Inject
    @AmazonClient("local")
    EcrClient namedClient;

    @ConfigProperty(name = "test.aws.requested-services")
    String services;

    @Test
    void disablesTheStackForAllClients() {
        assertEquals("[]", services);
        assertTrue(defaultClient.serviceClientConfiguration().endpointOverride().isEmpty());
        assertTrue(namedClient.serviceClientConfiguration().endpointOverride().isEmpty());
    }
}
