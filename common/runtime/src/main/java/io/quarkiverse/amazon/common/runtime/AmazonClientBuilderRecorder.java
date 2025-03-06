package io.quarkiverse.amazon.common.runtime;

import java.util.concurrent.Executor;

import io.quarkus.runtime.LaunchMode;
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
        if (transport != null) {
            builder.getValue().httpClientBuilder(transport.getValue());
        }
        return new RuntimeValue<>((AwsClientBuilder) builder.getValue());
    }

    public RuntimeValue<AwsClientBuilder> createAsyncBuilder(RuntimeValue<AwsAsyncClientBuilder<?, ?>> builder,
            RuntimeValue<SdkAsyncHttpClient.Builder> transport,
            LaunchMode launchMode, Executor executor, RuntimeValue<AsyncHttpClientConfig> config) {
        if (transport != null) {
            builder.getValue().httpClientBuilder(transport.getValue());
        }

        Executor configExecutor;

        if (!config.getValue().advanced().useFutureCompletionThreadPool()) {
            configExecutor = Runnable::run;
        } else {
            configExecutor = executor;
        }

        if (launchMode != LaunchMode.NORMAL) {
            configExecutor = new ClassLoaderExecutorWrapper(executor);
        }

        final Executor futureCompletionExecutor = configExecutor;

        builder.getValue().asyncConfiguration(asyncConfigBuilder -> asyncConfigBuilder
                .advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, futureCompletionExecutor));

        return new RuntimeValue<>((AwsClientBuilder) builder.getValue());
    }

    public RuntimeValue<SdkPresigner.Builder> createPresignerBuilder() {
        throw new UnsupportedOperationException();
    }

    /**
     * Capture the current ClassLoader and restore it to support dev and test mode
     */
    private static final class ClassLoaderExecutorWrapper implements Executor {

        private Executor executor;
        private ClassLoader contextClassLoader;

        private ClassLoaderExecutorWrapper(Executor executor) {
            this.executor = executor;
            this.contextClassLoader = Thread.currentThread().getContextClassLoader();
        }

        @Override
        public void execute(Runnable command) {
            final ClassLoader old = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
                executor.execute(command);
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        }
    }
}
