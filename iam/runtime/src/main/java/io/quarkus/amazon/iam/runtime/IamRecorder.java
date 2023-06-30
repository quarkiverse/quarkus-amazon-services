package io.quarkus.amazon.iam.runtime;

import java.util.concurrent.Executor;

import io.quarkus.amazon.common.runtime.AwsConfig;
import io.quarkus.amazon.common.runtime.NettyHttpClientConfig;
import io.quarkus.amazon.common.runtime.SdkConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.services.iam.IamAsyncClient;
import software.amazon.awssdk.services.iam.IamAsyncClientBuilder;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.IamClientBuilder;

@Recorder
public class IamRecorder {

    final IamConfig config;

    public IamRecorder(IamConfig config) {
        this.config = config;
    }

    public RuntimeValue<SyncHttpClientConfig> getSyncConfig() {
        return new RuntimeValue<>(config.syncClient);
    }

    public RuntimeValue<NettyHttpClientConfig> getAsyncConfig() {
        return new RuntimeValue<>(config.asyncClient);
    }

    public RuntimeValue<AwsConfig> getAwsConfig() {
        return new RuntimeValue<>(config.aws);
    }

    public RuntimeValue<SdkConfig> getSdkConfig() {
        return new RuntimeValue<>(config.sdk);
    }

    public RuntimeValue<AwsClientBuilder> createSyncBuilder(RuntimeValue<SdkHttpClient.Builder> transport) {
        IamClientBuilder builder = IamClient.builder();
        if (transport != null) {
            builder.httpClientBuilder(transport.getValue());
        }
        return new RuntimeValue<>(builder);
    }

    public RuntimeValue<AwsClientBuilder> createAsyncBuilder(RuntimeValue<SdkAsyncHttpClient.Builder> transport,
            Executor executor) {

        IamAsyncClientBuilder builder = IamAsyncClient.builder();
        if (transport != null) {
            builder.httpClientBuilder(transport.getValue());
        }
        if (!config.asyncClient.advanced.useFutureCompletionThreadPool) {
            builder.asyncConfiguration(asyncConfigBuilder -> asyncConfigBuilder
                    .advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, Runnable::run));
        } else {
            builder.asyncConfiguration(asyncConfigBuilder -> asyncConfigBuilder
                    .advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, executor));
        }
        return new RuntimeValue<>(builder);
    }
}
