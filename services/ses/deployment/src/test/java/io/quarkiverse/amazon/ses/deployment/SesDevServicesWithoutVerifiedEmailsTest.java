package io.quarkiverse.amazon.ses.deployment;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.ses.SesClient;

class SesDevServicesWithoutVerifiedEmailsTest {

    @Inject
    Instance<SesClient> client;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withEmptyApplication();

    @Test
    void test() {
        assertNotNull(client.get());
        assertTrue(client.get().listIdentities().identities().isEmpty());
    }
}
