package io.quarkiverse.amazon.common.runtime;

import io.quarkus.runtime.RuntimeValue;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.awscore.presigner.SdkPresigner;

public abstract class AmazonClientRecorder {

    public abstract <T> RuntimeValue<? extends HasAmazonClientRuntimeConfig> getAmazonClientsConfig();

    public abstract AsyncHttpClientConfig getAsyncClientConfig();

    public abstract SyncHttpClientConfig getSyncClientConfig();

    public abstract AwsSyncClientBuilder<?, ?> getSyncClientBuilder();

    public abstract AwsAsyncClientBuilder<?, ?> getAsyncClientBuilder();

    public RuntimeValue<SyncHttpClientConfig> getSyncConfig() {
        return new RuntimeValue<>(getSyncClientConfig());
    }

    public RuntimeValue<AsyncHttpClientConfig> getAsyncConfig() {
        return new RuntimeValue<>(getAsyncClientConfig());
    }

    public RuntimeValue<AwsSyncClientBuilder<?, ?>> getSyncBuilder() {
        return new RuntimeValue<>(getSyncClientBuilder());
    }

    public RuntimeValue<AwsAsyncClientBuilder<?, ?>> getAsyncBuilder() {
        return new RuntimeValue<>(getAsyncClientBuilder());
    }

    public RuntimeValue<SdkPresigner.Builder> createPresignerBuilder() {
        throw new UnsupportedOperationException();
    }
}
