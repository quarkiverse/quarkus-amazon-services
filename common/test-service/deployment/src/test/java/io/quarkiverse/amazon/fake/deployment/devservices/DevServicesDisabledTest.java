package io.quarkiverse.amazon.fake.deployment.devservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkiverse.amazon.fake.deployment.SdkHttpClients;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.fake.FakeClient;

/**
 * No stack is requested for a service whose dev services are disabled.
 */
public class DevServicesDisabledTest {

    @Inject
    FakeClient defaultClient;

    @Inject
    @AmazonClient("local")
    FakeClient namedClient;

    @ConfigProperty(name = StubAwsStackProcessor.REQUESTED_SERVICES)
    String requestedServices;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setForcedDependencies(SdkHttpClients.of(SdkHttpClients.URL_CONNECTION))
            .withApplicationRoot((jar) -> jar
                    .addClasses(StubAwsStackProcessor.class, SdkHttpClients.class)
                    .addAsResource(new StringAsset(StubAwsStackProcessor.class.getName()),
                            "META-INF/quarkus-build-steps.list"))
            // Disable real containers only; the stub still observes the service's provider requests.
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.fake-service.devservices.enabled", "false")
            .overrideConfigKey("quarkus.fake-service.aws.region", "us-east-1")
            .overrideConfigKey("quarkus.fake-service.aws.credentials.type", "anonymous");

    @Test
    public void disablesTheStackForAllClients() {
        assertEquals("[]", requestedServices);
        assertTrue(defaultClient.serviceClientConfiguration().endpointOverride().isEmpty());
        assertTrue(namedClient.serviceClientConfiguration().endpointOverride().isEmpty());
    }
}
