package io.quarkiverse.amazon.sesv2.deployment;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.IdentityInfo;
import software.amazon.awssdk.services.sesv2.model.ListEmailIdentitiesRequest;

class SesV2DevServicesWithVerifiedEmailsTest {

    private static final String SENDER = "sender@example.com";
    private static final String OTHER_SENDER = "other-sender@example.com";

    @Inject
    Instance<SesV2Client> client;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addAsResource(
                    new StringAsset("quarkus.sesv2.devservices.verified-emails=" + SENDER + "," + OTHER_SENDER),
                    "application.properties"));

    @Test
    void test() {
        assertNotNull(client.get());
        List<String> identities = client.get().listEmailIdentities(ListEmailIdentitiesRequest.builder().build())
                .emailIdentities().stream()
                .map(IdentityInfo::identityName)
                .toList();
        assertEquals(2, identities.size());
        assertTrue(identities.containsAll(List.of(SENDER, OTHER_SENDER)));

        assertTrue(client.get().getEmailIdentity(r -> r.emailIdentity(SENDER)).verifiedForSendingStatus());
        assertTrue(client.get().getEmailIdentity(r -> r.emailIdentity(OTHER_SENDER)).verifiedForSendingStatus());

        String messageId = client.get().sendEmail(r -> r
                .fromEmailAddress(SENDER)
                .destination(d -> d.toAddresses("recipient@example.com"))
                .content(c -> c.simple(m -> m
                        .subject(s -> s.data("Quarkus"))
                        .body(b -> b.text(t -> t.data("Quarkus is awesome"))))))
                .messageId();
        assertFalse(messageId.isBlank());
    }
}
