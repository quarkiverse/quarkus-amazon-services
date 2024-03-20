package io.quarkus.amazon.cloudwatch.runtime;

import io.quarkus.amazon.common.runtime.*;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;

@Recorder
public class CloudWatchLogsRecorder extends AmazonClientRecorder {

    final CloudWatchLogsConfig config;

    public CloudWatchLogsRecorder(CloudWatchLogsConfig config) {
        this.config = config;
    }

    @Override
    public RuntimeValue<AwsConfig> getAwsConfig() {
        return new RuntimeValue<>(config.aws());
    }

    @Override
    public RuntimeValue<SdkConfig> getSdkConfig() {
        return new RuntimeValue<>(config.sdk());
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
        return CloudWatchLogsClient.builder();
    }

    @Override
    public AwsAsyncClientBuilder<?, ?> getAsyncClientBuilder() {
        return CloudWatchLogsAsyncClient.builder();
    }
}
