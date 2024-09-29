package io.quarkiverse.amazon.sqs.deployment;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.AmazonClient;
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
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.aws.devservices.localstack.image-name=localstack/localstack:3.0.1"),
                            "application.properties"));

    @Test
    void test() {
        assertNotNull(client.get());
        assertTrue(client.get().listQueues().queueUrls().isEmpty());
        assertNotNull(clientNamed.get());
        assertTrue(clientNamed.get().listQueues().queueUrls().isEmpty());
    }
}
