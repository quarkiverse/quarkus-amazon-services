package io.quarkiverse.amazon.common.runtime;

import java.util.concurrent.Executor;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.awscore.presigner.SdkPresigner;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

@Recorder
public class AmazonClientBuilderRecorder {

    public RuntimeValue<AwsClientBuilder> createSyncBuilder(RuntimeValue<AwsSyncClientBuilder<?, ?>> builder,
            RuntimeValue<SdkHttpClient.Builder> transport) {
        AwsSyncClientBuilder<?, ?> sdkBuilder = builder.getValue();
        if (transport != null) {
            sdkBuilder.httpClientBuilder(transport.getValue());
        }
        return new RuntimeValue<>((AwsClientBuilder) sdkBuilder);
    }

    public RuntimeValue<AwsClientBuilder> createAsyncBuilder(RuntimeValue<AwsAsyncClientBuilder<?, ?>> builder,
            RuntimeValue<SdkAsyncHttpClient.Builder> transport,
            Executor executor, RuntimeValue<AsyncHttpClientConfig> config) {
        AwsAsyncClientBuilder<?, ?> sdkBuilder = builder.getValue();
        if (transport != null) {
            sdkBuilder.httpClientBuilder(transport.getValue());
        }

        Executor futureCompletionExecutor = AsyncClientCompletion.futureCompletionExecutor(config.getValue(), executor);
        sdkBuilder.asyncConfiguration(asyncConfigBuilder -> asyncConfigBuilder
                .advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, futureCompletionExecutor));

        return new RuntimeValue<>((AwsClientBuilder) sdkBuilder);
    }

    /**
     * Only recorded when the Vert.x extension is present, see {@link AsyncClientCompletion}.
     */
    public void propagateVertxContext(RuntimeValue<AwsClientBuilder> builder, RuntimeValue<AsyncHttpClientConfig> config) {
        if (config.getValue().advanced().vertxContextPropagation()) {
            // A plugin, unlike the override configuration, survives the application replacing the configuration
            builder.getValue().addPlugin(new VertxContextPropagationPlugin(new VertxContextCapturingInterceptor(true)));
        }
    }

    public RuntimeValue<SdkPresigner.Builder> createPresignerBuilder() {
        throw new UnsupportedOperationException();
    }
}
