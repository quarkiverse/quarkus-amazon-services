package io.quarkiverse.amazon.ecr.runtime;

import io.quarkiverse.amazon.common.runtime.AmazonClientRecorder;
import io.quarkiverse.amazon.common.runtime.AsyncHttpClientConfig;
import io.quarkiverse.amazon.common.runtime.HasAmazonClientRuntimeConfig;
import io.quarkiverse.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.services.ecr.EcrAsyncClient;
import software.amazon.awssdk.services.ecr.EcrClient;

@Generated("io.quarkiverse.amazon:codegen")
@Recorder
public class EcrRecorder extends AmazonClientRecorder {
    final EcrConfig config;

    public EcrRecorder(EcrConfig config) {
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
    public AwsSyncClientBuilder<?, ?> getSyncClientBuilder() {
        var builder = EcrClient.builder();
        return builder;
    }

    @Override
    public AwsAsyncClientBuilder<?, ?> getAsyncClientBuilder() {
        var builder = EcrAsyncClient.builder();
        return builder;
    }
}