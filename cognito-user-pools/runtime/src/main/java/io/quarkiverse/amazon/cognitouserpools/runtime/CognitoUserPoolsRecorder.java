package io.quarkiverse.amazon.cognitouserpools.runtime;

import io.quarkiverse.amazon.common.runtime.AmazonClientRecorder;
import io.quarkiverse.amazon.common.runtime.AsyncHttpClientConfig;
import io.quarkiverse.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Recorder
public class CognitoUserPoolsRecorder extends AmazonClientRecorder {
    final RuntimeValue<CognitoUserPoolsConfig> config;

    public CognitoUserPoolsRecorder(RuntimeValue<CognitoUserPoolsConfig> config) {
        this.config = config;
    }

    @Override
    public RuntimeValue<CognitoUserPoolsConfig> getAmazonClientsConfig() {
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
        return CognitoIdentityProviderClient.builder();
    }

    @Override
    public AwsAsyncClientBuilder<?, ?> getAsyncClientBuilder() {
        return CognitoIdentityProviderAsyncClient.builder();
    }
}
