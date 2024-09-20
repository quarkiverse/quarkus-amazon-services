package io.quarkus.amazon.common.runtime;

import java.util.concurrent.Executor;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.awscore.presigner.SdkPresigner;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.http.SdkHttpClient.Builder;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

public abstract class AmazonClientRecorder {

    public abstract RuntimeValue<HasAmazonClientRuntimeConfig> getAmazonClientsConfig();

    public abstract AsyncHttpClientConfig getAsyncClientConfig();

    public abstract SyncHttpClientConfig getSyncClientConfig();

    public abstract AwsSyncClientBuilder<?, ?> geSyncClientBuilder();

    public abstract AwsAsyncClientBuilder<?, ?> getAsyncClientBuilder();

    public RuntimeValue<SyncHttpClientConfig> getSyncConfig() {
        return new RuntimeValue<>(getSyncClientConfig());
    }

    public RuntimeValue<AsyncHttpClientConfig> getAsyncConfig() {
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
            LaunchMode launchMode,
            Executor executor) {
        AwsAsyncClientBuilder<?, ?> builder = getAsyncClientBuilder();
        AsyncHttpClientConfig config = getAsyncClientConfig();

        if (transport != null) {
            builder.httpClientBuilder(transport.getValue());
        }

        Executor configExecutor;

        if (!config.advanced().useFutureCompletionThreadPool()) {
            configExecutor = Runnable::run;
        } else {
            configExecutor = executor;
        }

        if (launchMode != LaunchMode.NORMAL) {
            configExecutor = new ClassLoaderExecutorWrapper(executor);
        }

        final Executor futureCompletionExecutor = configExecutor;

        builder.asyncConfiguration(asyncConfigBuilder -> asyncConfigBuilder
                .advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, futureCompletionExecutor));

        return new RuntimeValue<>((AwsClientBuilder) builder);
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
