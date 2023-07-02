package io.quarkus.amazon.s3.runtime;

import io.quarkus.amazon.common.runtime.AmazonClientRecorder;
import io.quarkus.amazon.common.runtime.AsyncHttpClientConfig;
import io.quarkus.amazon.common.runtime.AwsConfig;
import io.quarkus.amazon.common.runtime.SdkConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.awscore.presigner.SdkPresigner;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3BaseClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Recorder
public class S3Recorder extends AmazonClientRecorder {

    final S3Config config;

    public S3Recorder(S3Config config) {
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
    public AsyncHttpClientConfig getAsyncClientConfig() {
        return config.asyncClient;
    }

    @Override
    public SyncHttpClientConfig getSyncClientConfig() {
        return config.syncClient;
    }

    @Override
    public AwsSyncClientBuilder<?, ?> geSyncClientBuilder() {
        S3ClientBuilder builder = S3Client.builder();
        configureS3Client(builder);

        return builder;
    }

    @Override
    public AwsAsyncClientBuilder<?, ?> getAsyncClientBuilder() {
        S3AsyncClientBuilder builder = S3AsyncClient.builder();
        configureS3Client(builder);

        return builder;
    }

    @Override
    public RuntimeValue<SdkPresigner.Builder> createPresignerBuilder() {
        S3Presigner.Builder builder = S3Presigner.builder()
                .serviceConfiguration(s3ConfigurationBuilder().build())
                .dualstackEnabled(config.dualstack);
        return new RuntimeValue<>(builder);
    }

    private void configureS3Client(S3BaseClientBuilder builder) {
        builder
                .serviceConfiguration(s3ConfigurationBuilder().build())
                .dualstackEnabled(config.dualstack);
    }

    private S3Configuration.Builder s3ConfigurationBuilder() {
        S3Configuration.Builder s3ConfigBuilder = S3Configuration.builder()
                .accelerateModeEnabled(config.accelerateMode)
                .checksumValidationEnabled(config.checksumValidation)
                .chunkedEncodingEnabled(config.chunkedEncoding)
                .pathStyleAccessEnabled(config.pathStyleAccess)
                .useArnRegionEnabled(config.useArnRegionEnabled);
        config.profileName.ifPresent(s3ConfigBuilder::profileName);
        return s3ConfigBuilder;
    }
}
