package io.quarkus.amazon.dynamodb.runtime;

import io.quarkus.amazon.common.runtime.AmazonClientRecorder;
import io.quarkus.amazon.common.runtime.AsyncHttpClientConfig;
import io.quarkus.amazon.common.runtime.HasAmazonClientRuntimeConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Recorder
public class DynamodbRecorder extends AmazonClientRecorder {

    final DynamodbConfig config;

    public DynamodbRecorder(DynamodbConfig config) {
        this.config = config;
    }

    @Override
    public RuntimeValue<HasAmazonClientRuntimeConfig> getAmazonClientsConfig() {
        return new RuntimeValue<>(config);
    }

    @Override
    public AsyncHttpClientConfig getAsyncClientConfig() {
        return config.asyncClient();
    }

    @Override
    public SyncHttpClientConfig getSyncClientConfig() {
        return config.syncClient();
    }

    @Override
    public AwsSyncClientBuilder<?, ?> geSyncClientBuilder() {
        return DynamoDbClient.builder();
    }

    @Override
    public AwsAsyncClientBuilder<?, ?> getAsyncClientBuilder() {
        return DynamoDbAsyncClient.builder();
    }
}
