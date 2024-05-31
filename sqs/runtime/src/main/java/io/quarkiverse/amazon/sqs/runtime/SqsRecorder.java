package io.quarkiverse.amazon.sqs.runtime;

import io.quarkiverse.amazon.common.runtime.AmazonClientRecorder;
import io.quarkiverse.amazon.common.runtime.AsyncHttpClientConfig;
import io.quarkiverse.amazon.common.runtime.AwsConfig;
import io.quarkiverse.amazon.common.runtime.SdkConfig;
import io.quarkiverse.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@Recorder
public class SqsRecorder extends AmazonClientRecorder {

    final SqsConfig config;

    public SqsRecorder(SqsConfig config) {
        this.config = config;
    }

    @Override
    public RuntimeValue<AwsConfig> getAwsConfig() {
        return new RuntimeValue<>(config.aws());
    }

    @Override
    public RuntimeValue<SdkConfig> getSdkConfig() {
        return new RuntimeValue<>(config.sdk());
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
        return SqsClient.builder();
    }

    @Override
    public AwsAsyncClientBuilder<?, ?> getAsyncClientBuilder() {
        return SqsAsyncClient.builder();
    }
}
