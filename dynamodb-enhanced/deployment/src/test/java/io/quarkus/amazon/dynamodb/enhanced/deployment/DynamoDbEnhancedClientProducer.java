package io.quarkus.amazon.dynamodb.enhanced.deployment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DynamoDbEnhancedClientProducer {

    @Produces
    @ApplicationScoped
    public DynamoDbEnhancedClient enhancedClient(DynamoDbClient client) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(client)
                .build();
    }

    @Produces
    @ApplicationScoped
    public DynamoDbClient client() {
        return DynamoDbClient.create();
    }
}
