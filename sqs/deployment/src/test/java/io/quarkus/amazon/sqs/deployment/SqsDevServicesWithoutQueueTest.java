package io.quarkus.amazon.sqs.deployment;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.sqs.SqsClient;

class SqsDevServicesWithoutQueueTest {

    @Inject
    Instance<SqsClient> client;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.aws.devservices.localstack.image-name=localstack/localstack:1.0.3"),
                            "application.properties"));

    @Test
    void test() {
        assertNotNull(client.get());
        assertTrue(client.get().listQueues().queueUrls().isEmpty());
    }
}
