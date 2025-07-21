package io.quarkiverse.amazon.sqs.runtime;

import io.quarkiverse.amazon.common.runtime.AmazonClientRecorder;
import io.quarkiverse.amazon.common.runtime.AsyncHttpClientConfig;
import io.quarkiverse.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@Recorder
public class SqsRecorder extends AmazonClientRecorder {

    final RuntimeValue<SqsConfig> config;

    public SqsRecorder(RuntimeValue<SqsConfig> config) {
        this.config = config;
    }

    @Override
    public RuntimeValue<SqsConfig> getAmazonClientsConfig() {
        return config;
    }

    @Override
    public AsyncHttpClientConfig getAsyncClientConfig() {
        return config.getValue().asyncClient();
    }

    @Override
    public SyncHttpClientConfig getSyncClientConfig() {
        return config.getValue().syncClient();
    }

    @Override
    public AwsSyncClientBuilder<?, ?> getSyncClientBuilder() {
        return SqsClient.builder();
    }

    @Override
    public AwsAsyncClientBuilder<?, ?> getAsyncClientBuilder() {
        return SqsAsyncClient.builder();
    }
}
