package io.quarkus.amazon.secretsmanager.runtime;

import io.quarkus.amazon.common.runtime.AmazonClientRecorder;
import io.quarkus.amazon.common.runtime.AsyncHttpClientConfig;
import io.quarkus.amazon.common.runtime.HasAmazonClientRuntimeConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

@Recorder
public class SecretsManagerRecorder extends AmazonClientRecorder {

    final SecretsManagerConfig config;

    public SecretsManagerRecorder(SecretsManagerConfig config) {
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
        return SecretsManagerClient.builder();
    }

    @Override
    public AwsAsyncClientBuilder<?, ?> getAsyncClientBuilder() {
        return SecretsManagerAsyncClient.builder();
    }
}
