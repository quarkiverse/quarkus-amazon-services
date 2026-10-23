package io.quarkiverse.amazon.ses.deployment;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.IdentityType;
import software.amazon.awssdk.services.ses.model.IdentityVerificationAttributes;
import software.amazon.awssdk.services.ses.model.VerificationStatus;

class SesDevServicesWithVerifiedEmailsTest {

    private static final String SENDER = "sender@example.com";
    private static final String OTHER_SENDER = "other-sender@example.com";

    @Inject
    Instance<SesClient> client;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addAsResource(
                    new StringAsset("quarkus.ses.devservices.verified-emails=" + SENDER + "," + OTHER_SENDER),
                    "application.properties"));

    @Test
    void test() {
        assertNotNull(client.get());
        List<String> identities = client.get().listIdentities(r -> r.identityType(IdentityType.EMAIL_ADDRESS)).identities();
        assertEquals(2, identities.size());
        assertTrue(identities.containsAll(List.of(SENDER, OTHER_SENDER)));

        Map<String, IdentityVerificationAttributes> attributes = client.get()
                .getIdentityVerificationAttributes(r -> r.identities(identities)).verificationAttributes();
        assertEquals(VerificationStatus.SUCCESS, attributes.get(SENDER).verificationStatus());
        assertEquals(VerificationStatus.SUCCESS, attributes.get(OTHER_SENDER).verificationStatus());

        String messageId = client.get().sendEmail(r -> r
                .source(SENDER)
                .destination(d -> d.toAddresses("recipient@example.com"))
                .message(m -> m
                        .subject(s -> s.data("Quarkus"))
                        .body(b -> b.text(t -> t.data("Quarkus is awesome")))))
                .messageId();
        assertFalse(messageId.isBlank());
    }
}
