package io.quarkus.amazon.dynamodb.enhanced.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.quarkus.arc.DefaultBean;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@ApplicationScoped
public class DynamodbEnhancedClientProducer {

    private DynamoDbEnhancedClient syncEnhancedClient;

    private DynamoDbEnhancedAsyncClient asyncEnhancedClient;

    public void setDynamoDbClient(DynamoDbClient client) {
        syncEnhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
    }

    public void setDynamoDbAsyncClient(DynamoDbAsyncClient client) {
        asyncEnhancedClient = DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(client).build();
    }

    @DefaultBean
    @Produces
    @ApplicationScoped
    public DynamoDbEnhancedClient client() {
        if (syncEnhancedClient == null) {
            throw new IllegalStateException("The DynamoDbEnhancedClient is required no DynamoDB client has been configured.");
        }
        return syncEnhancedClient;
    }

    @DefaultBean
    @Produces
    @ApplicationScoped
    public DynamoDbEnhancedAsyncClient asyncClient() {
        if (asyncEnhancedClient == null) {
            throw new IllegalStateException(
                    "The DynamoDbEnhancedAsyncClient is required but no DynamoDB async client has been configured.");
        }
        return asyncEnhancedClient;
    }

}
