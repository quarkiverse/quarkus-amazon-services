package io.quarkus.amazon.common.runtime;

import java.util.concurrent.Executor;

import io.quarkus.runtime.RuntimeValue;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.awscore.presigner.SdkPresigner;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.http.SdkHttpClient.Builder;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

public abstract class AmazonClientRecorder {

    public abstract RuntimeValue<AwsConfig> getAwsConfig();

    public abstract RuntimeValue<SdkConfig> getSdkConfig();

    public abstract NettyHttpClientConfig getAsyncClientConfig();

    public abstract SyncHttpClientConfig getSyncClientConfig();

    public abstract AwsSyncClientBuilder<?, ?> geSyncClientBuilder();

    public abstract AwsAsyncClientBuilder<?, ?> getAsyncClientBuilder();

    public RuntimeValue<SyncHttpClientConfig> getSyncConfig() {
        return new RuntimeValue<>(getSyncClientConfig());
    }

    public RuntimeValue<NettyHttpClientConfig> getAsyncConfig() {
        return new RuntimeValue<>(getAsyncClientConfig());
    }

    public RuntimeValue<AwsClientBuilder> createSyncBuilder(RuntimeValue<Builder> transport) {
        AwsSyncClientBuilder<?, ?> builder = geSyncClientBuilder();

        if (transport != null) {
            builder.httpClientBuilder(transport.getValue());
        }
        return new RuntimeValue<>((AwsClientBuilder) builder);
    }

    public RuntimeValue<AwsClientBuilder> createAsyncBuilder(RuntimeValue<SdkAsyncHttpClient.Builder> transport,
            Executor executor) {
        AwsAsyncClientBuilder<?, ?> builder = getAsyncClientBuilder();
        NettyHttpClientConfig config = getAsyncClientConfig();

        if (transport != null) {
            builder.httpClientBuilder(transport.getValue());
        }
        if (!config.advanced.useFutureCompletionThreadPool) {
            builder.asyncConfiguration(asyncConfigBuilder -> asyncConfigBuilder
                    .advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, Runnable::run));
        } else {
            builder.asyncConfiguration(asyncConfigBuilder -> asyncConfigBuilder
                    .advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, executor));
        }
        return new RuntimeValue<>((AwsClientBuilder) builder);
    }

    public RuntimeValue<SdkPresigner.Builder> createPresignerBuilder() {
        throw new UnsupportedOperationException();
    }
}
