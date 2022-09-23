package io.quarkus.amazon.dynamodb.enhanced.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@ApplicationScoped
public class DynamodbEnhancedClientProducer {

    private DynamoDbEnhancedClient syncEnhancedClient;

    private DynamoDbEnhancedAsyncClient asyncEnhancedClient;

    DynamodbEnhancedClientProducer(Instance<DynamoDbClient> syncClientInstance,
            Instance<DynamoDbAsyncClient> asyncClientInstance) {
        if (syncClientInstance.isResolvable()) {
            this.syncEnhancedClient = DynamoDbEnhancedClient.builder()
                    .dynamoDbClient(syncClientInstance.get()).build();
        }
        if (asyncClientInstance.isResolvable()) {
            this.asyncEnhancedClient = DynamoDbEnhancedAsyncClient.builder()
                    .dynamoDbClient(asyncClientInstance.get()).build();
        }
    }

    @Produces
    @ApplicationScoped
    public DynamoDbEnhancedClient client() {
        if (syncEnhancedClient == null) {
            throw new IllegalStateException("The DynamoDbEnhancedClient is required no DynamoDB client has been configured.");
        }
        return syncEnhancedClient;
    }

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
