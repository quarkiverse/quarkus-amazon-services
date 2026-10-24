package io.quarkiverse.amazon.fake.deployment.devservices;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * No stack is requested for a service whose endpoint is configured.
 */
public class DevServicesWithEndpointOverrideTest {

    @ConfigProperty(name = StubAwsStackProcessor.REQUESTED_SERVICES)
    String requestedServices;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(StubAwsStackProcessor.class)
                    .addAsResource(new StringAsset(StubAwsStackProcessor.class.getName()),
                            "META-INF/quarkus-build-steps.list"))
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.fake-service.endpoint-override", "http://localhost:9090");

    @Test
    public void test() {
        assertEquals("[]", requestedServices);
    }
}
