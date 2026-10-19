package io.quarkiverse.amazon.ses.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.ses.SesClient;

class SesDevServicesWithVerifiedEmailsTest {

    @Inject
    Instance<SesClient> client;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addAsResource(
                    new StringAsset("quarkus.ses.devservices.verified-emails=from-quarkus@example.com,second@example.com"),
                    "application.properties"));

    @Test
    void test() {
        assertNotNull(client.get());
        Set<String> identities = new HashSet<>(client.get().listIdentities().identities());
        assertEquals(Set.of("from-quarkus@example.com", "second@example.com"), identities);
    }
}
