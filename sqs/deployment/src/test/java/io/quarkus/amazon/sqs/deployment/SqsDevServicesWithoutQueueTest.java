package io.quarkus.amazon.sqs.deployment;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.amazon.common.AmazonClient;
import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.sqs.SqsClient;

class SqsDevServicesWithoutQueueTest {

    @Inject
    Instance<SqsClient> client;

    @Inject
    @AmazonClient("test")
    Instance<SqsClient> clientNamed;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication();

    @Test
    void test() {
        assertNotNull(client.get());
        assertTrue(client.get().listQueues().queueUrls().isEmpty());
        assertNotNull(clientNamed.get());
        assertTrue(clientNamed.get().listQueues().queueUrls().isEmpty());
    }
}
