package io.quarkiverse.amazon.dynamodb.enhanced.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.AmazonClient;
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
        assertNotEquals(ClientProxy.unwrap(clientNamedTest), ClientProxy.unwrap(client));
        assertEquals(ClientProxy.unwrap(clientSame), ClientProxy.unwrap(client));
        assertEquals(ClientProxy.unwrap(clientNamedTestSame), ClientProxy.unwrap(clientNamedTest));
    }
}
