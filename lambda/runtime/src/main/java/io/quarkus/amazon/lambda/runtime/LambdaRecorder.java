package io.quarkus.amazon.lambda.runtime;

import io.quarkus.amazon.common.runtime.*;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.services.lambda.*;

@Recorder
public class LambdaRecorder extends AmazonClientRecorder {

    final LambdaConfig config;

    public LambdaRecorder(final LambdaConfig config) {
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
        return LambdaClient.builder();
    }

    @Override
    public AwsAsyncClientBuilder<?, ?> getAsyncClientBuilder() {
        return LambdaAsyncClient.builder();
    }

    private LambdaServiceClientConfiguration.Builder lambdaConfigurationBuilder() {
        final LambdaServiceClientConfiguration.Builder lambdaConfigBuilder = LambdaServiceClientConfiguration.builder();
        return lambdaConfigBuilder;
    }
}
