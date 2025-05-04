package io.quarkiverse.amazon.ssooidc.runtime;

import io.quarkiverse.amazon.common.runtime.AmazonClientRecorder;
import io.quarkiverse.amazon.common.runtime.AsyncHttpClientConfig;
import io.quarkiverse.amazon.common.runtime.HasAmazonClientRuntimeConfig;
import io.quarkiverse.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.services.ssooidc.SsoOidcAsyncClient;
import software.amazon.awssdk.services.ssooidc.SsoOidcClient;

@Recorder
public class SsoOidcRecorder extends AmazonClientRecorder {

    final SsoOidcConfig config;

    public SsoOidcRecorder(SsoOidcConfig config) {
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
        return SsoOidcClient.builder();
    }

    @Override
    public AwsAsyncClientBuilder<?, ?> getAsyncClientBuilder() {
        return SsoOidcAsyncClient.builder();
    }
}
