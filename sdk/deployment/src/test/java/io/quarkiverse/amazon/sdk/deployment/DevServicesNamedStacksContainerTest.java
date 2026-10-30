package io.quarkiverse.amazon.sdk.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.sesv2.SesV2AsyncClient;
import software.amazon.awssdk.services.sesv2.SesV2Client;

/**
 * Verifies default and named clients use independent Floci and Moto stacks, with consistent sync/async routing and isolated
 * data.
 */
@EnabledIfSystemProperty(named = "test.aws.devservices.container", matches = "true")
class DevServicesNamedStacksContainerTest {
    @RegisterExtension
    static final QuarkusExtensionTest app = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(DevServicesNamedStacksContainerTest.class))
            .overrideConfigKey("quarkus.aws.devservices.provider", "floci")
            .overrideConfigKey("quarkus.aws.\"other\".devservices.provider", "moto")
            .overrideConfigKey("quarkus.aws.devservices.shared", "false");

    @Inject
    SesV2Client sync;
    @Inject
    SesV2AsyncClient async;
    @Inject
    @AmazonClient("other")
    SesV2Client otherSync;
    @Inject
    @AmazonClient("other")
    SesV2AsyncClient otherAsync;

    @Test
    void routesDefaultAndNamedClientsToIndependentStacks() {
        assertNotEquals(sync.serviceClientConfiguration().endpointOverride(),
                otherSync.serviceClientConfiguration().endpointOverride());
        assertEquals(sync.serviceClientConfiguration().endpointOverride(),
                async.serviceClientConfiguration().endpointOverride());
        assertEquals(otherSync.serviceClientConfiguration().endpointOverride(),
                otherAsync.serviceClientConfiguration().endpointOverride());
        String identity = "named-stacks@example.com";
        sync.createEmailIdentity(request -> request.emailIdentity(identity));
        try {
            assertEquals(1, async.listEmailIdentities(request -> {
            }).join().emailIdentities().size());
            assertEquals(0, otherAsync.listEmailIdentities(request -> {
            }).join().emailIdentities().size());
            otherAsync.createEmailIdentity(request -> request.emailIdentity(identity)).join();
            try {
                assertEquals(1, otherSync.listEmailIdentities(request -> {
                }).emailIdentities().size());
            } finally {
                otherSync.deleteEmailIdentity(request -> request.emailIdentity(identity));
            }
        } finally {
            sync.deleteEmailIdentity(request -> request.emailIdentity(identity));
        }
    }
}
