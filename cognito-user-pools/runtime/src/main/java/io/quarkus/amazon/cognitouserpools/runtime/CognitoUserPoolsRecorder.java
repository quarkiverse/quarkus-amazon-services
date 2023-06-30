package io.quarkus.amazon.cognitouserpools.runtime;

import io.quarkus.amazon.common.runtime.AmazonClientRecorder;
import io.quarkus.amazon.common.runtime.AwsConfig;
import io.quarkus.amazon.common.runtime.NettyHttpClientConfig;
import io.quarkus.amazon.common.runtime.SdkConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Recorder
public class CognitoUserPoolsRecorder extends AmazonClientRecorder {
    final CognitoUserPoolsConfig config;

    public CognitoUserPoolsRecorder(CognitoUserPoolsConfig config) {
        this.config = config;
    }

    @Override
    public RuntimeValue<AwsConfig> getAwsConfig() {
        return new RuntimeValue<>(config.aws);
    }

    @Override
    public RuntimeValue<SdkConfig> getSdkConfig() {
        return new RuntimeValue<>(config.sdk);
    }

    @Override
    public NettyHttpClientConfig getAsyncClientConfig() {
        return config.asyncClient;
    }

    @Override
    public SyncHttpClientConfig getSyncClientConfig() {
        return config.syncClient;
    }

    @Override
    public AwsSyncClientBuilder<?, ?> geSyncClientBuilder() {
        return CognitoIdentityProviderClient.builder();
    }

    @Override
    public AwsAsyncClientBuilder<?, ?> getAsyncClientBuilder() {
        return CognitoIdentityProviderAsyncClient.builder();
    }
}
