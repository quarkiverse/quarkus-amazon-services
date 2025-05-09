package io.quarkiverse.amazon.scheduler.runtime;

import io.quarkiverse.amazon.common.runtime.AmazonClientRecorder;
import io.quarkiverse.amazon.common.runtime.AsyncHttpClientConfig;
import io.quarkiverse.amazon.common.runtime.HasAmazonClientRuntimeConfig;
import io.quarkiverse.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.services.scheduler.SchedulerAsyncClient;
import software.amazon.awssdk.services.scheduler.SchedulerClient;

@Recorder
public class SchedulerRecorder extends AmazonClientRecorder {

    final SchedulerConfig config;

    public SchedulerRecorder(SchedulerConfig config) {
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
    public AwsSyncClientBuilder<?, ?> getSyncClientBuilder() {
        return SchedulerClient.builder();
    }

    @Override
    public AwsAsyncClientBuilder<?, ?> getAsyncClientBuilder() {
        return SchedulerAsyncClient.builder();
    }
}
