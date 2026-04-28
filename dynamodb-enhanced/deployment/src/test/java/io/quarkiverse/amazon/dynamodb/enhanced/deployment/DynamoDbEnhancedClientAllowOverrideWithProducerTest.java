package io.quarkiverse.amazon.dynamodb.enhanced.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

public class DynamoDbEnhancedClientAllowOverrideWithProducerTest {

    @Inject
    DynamoDbEnhancedClient client;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DynamoDbEnhancedClientProducer.class)
                    .addAsResource("full-config.properties", "application.properties"));

    @Test
    public void test() {
        // should not fail
    }
}
