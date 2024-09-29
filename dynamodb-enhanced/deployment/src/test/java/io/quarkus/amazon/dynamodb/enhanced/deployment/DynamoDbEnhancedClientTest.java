package io.quarkus.amazon.dynamodb.enhanced.deployment;

import jakarta.inject.Inject;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.amazon.common.AmazonClient;
import io.quarkus.arc.ClientProxy;
import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

public class DynamoDbEnhancedClientTest {

    @Inject
    DynamoDbEnhancedClient client;

    @Inject
    DynamoDbEnhancedClient clientSame;

    @Inject
    @AmazonClient("test")
    DynamoDbEnhancedClient clientNamedTest;

    @Inject
    @AmazonClient("test")
    DynamoDbEnhancedClient clientNamedTestSame;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DynamoDbEnhancedClientProducer.class)
                    .addAsResource("full-config.properties", "application.properties"));

    @Test
    public void test() {
        Assert.assertNotEquals(ClientProxy.unwrap(clientNamedTest), ClientProxy.unwrap(client));
        Assert.assertEquals(ClientProxy.unwrap(clientSame), ClientProxy.unwrap(client));
        Assert.assertEquals(ClientProxy.unwrap(clientNamedTestSame), ClientProxy.unwrap(clientNamedTest));
    }
}
