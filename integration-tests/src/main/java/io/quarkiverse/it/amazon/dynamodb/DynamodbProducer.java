package io.quarkiverse.it.amazon.dynamodb;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

@ApplicationScoped
public class DynamodbProducer {

    private static final DynamoDBModifyResponse EXECUTION_INTERCEPTOR = new io.quarkiverse.it.amazon.dynamodb.DynamoDBModifyResponse();

    @Produces
    @ApplicationScoped
    public DynamoDbClient createDynamoDbClient(DynamoDbClientBuilder builder) {
        builder.overrideConfiguration(
                c -> c.addExecutionInterceptor(EXECUTION_INTERCEPTOR));

        return builder.build();
    }

    @Produces
    @ApplicationScoped
    public DynamoDbAsyncClient createDynamoDbClient(DynamoDbAsyncClientBuilder builder) {
        builder.overrideConfiguration(
                c -> c.addExecutionInterceptor(EXECUTION_INTERCEPTOR));

        return builder.build();
    }
}
