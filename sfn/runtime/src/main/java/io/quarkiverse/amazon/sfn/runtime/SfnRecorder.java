package io.quarkiverse.amazon.sfn.runtime;

import io.quarkiverse.amazon.common.runtime.AmazonClientRecorder;
import io.quarkiverse.amazon.common.runtime.AsyncHttpClientConfig;
import io.quarkiverse.amazon.common.runtime.AwsConfig;
import io.quarkiverse.amazon.common.runtime.SdkConfig;
import io.quarkiverse.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.services.sfn.SfnAsyncClient;
import software.amazon.awssdk.services.sfn.SfnClient;

@Recorder
public class SfnRecorder extends AmazonClientRecorder {

    final SfnConfig config;

    public SfnRecorder(SfnConfig config) {
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
        return SfnClient.builder();
    }

    @Override
    public AwsAsyncClientBuilder<?, ?> getAsyncClientBuilder() {
        return SfnAsyncClient.builder();
    }
}
