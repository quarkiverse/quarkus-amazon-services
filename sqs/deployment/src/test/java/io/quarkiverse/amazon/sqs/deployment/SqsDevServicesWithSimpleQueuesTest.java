package io.quarkiverse.amazon.sqs.deployment;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.sqs.SqsClient;

class SqsDevServicesWithSimpleQueuesTest {

    @Inject
    Instance<SqsClient> client;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addAsResource(
                    new StringAsset("quarkus.sqs.devservices.queues=queue1,queue2"),
                    "application.properties"));

    @Test
    void test() {
        assertNotNull(client.get());
        List<String> queueUrls = client.get().listQueues().queueUrls();
        assertEquals(2, queueUrls.size());
        assertTrue(queueUrls.get(0).endsWith("/queue1"));
        assertTrue(queueUrls.get(1).endsWith("/queue2"));
    }
}
