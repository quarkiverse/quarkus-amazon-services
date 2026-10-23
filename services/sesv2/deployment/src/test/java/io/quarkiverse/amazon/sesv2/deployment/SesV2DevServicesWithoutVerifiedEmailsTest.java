package io.quarkiverse.amazon.sesv2.deployment;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.ListEmailIdentitiesRequest;

class SesV2DevServicesWithoutVerifiedEmailsTest {

    @Inject
    Instance<SesV2Client> client;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withEmptyApplication();

    @Test
    void test() {
        assertNotNull(client.get());
        assertTrue(client.get().listEmailIdentities(ListEmailIdentitiesRequest.builder().build()).emailIdentities().isEmpty());
    }
}
