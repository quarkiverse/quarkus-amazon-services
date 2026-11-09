package io.quarkiverse.amazon.s3.runtime;

import io.quarkiverse.amazon.common.runtime.AmazonClientRecorder;
import io.quarkiverse.amazon.common.runtime.AsyncHttpClientConfig;
import io.quarkiverse.amazon.common.runtime.SyncHttpClientConfig;
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
import software.amazon.awssdk.services.s3.multipart.MultipartConfiguration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.utils.Validate;

@Recorder
public class S3Recorder extends AmazonClientRecorder {

    final RuntimeValue<S3Config> config;

    public S3Recorder(RuntimeValue<S3Config> config) {
        this.config = config;
    }

    @Override
    public RuntimeValue<S3Config> getAmazonClientsConfig() {
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
        S3ClientBuilder builder = S3Client.builder();
        configureS3Client(builder);
        return builder;
    }

    @Override
    public AwsAsyncClientBuilder<?, ?> getAsyncClientBuilder() {
        S3AsyncClientBuilder builder = S3AsyncClient.builder();
        configureS3Client(builder);

        S3Config.MultipartConfig multipart = config.getValue().multipart();
        if (multipart.enabled()) {
            builder.multipartEnabled(true);
            MultipartConfiguration.Builder multipartBuilder = MultipartConfiguration.builder();
            multipart.thresholdInBytes().ifPresent(value -> multipartBuilder
                    .thresholdInBytes(Validate.isPositive(value.asLongValue(), "quarkus.s3.multipart.threshold-in-bytes")));
            multipart.minimumPartSizeInBytes().ifPresent(value -> multipartBuilder
                    .minimumPartSizeInBytes(
                            Validate.isPositive(value.asLongValue(), "quarkus.s3.multipart.minimum-part-size-in-bytes")));
            builder.multipartConfiguration(multipartBuilder.build());
        }

        return builder;
    }

    @Override
    public RuntimeValue<SdkPresigner.Builder> createPresignerBuilder() {
        S3Presigner.Builder builder = S3Presigner.builder()
                .serviceConfiguration(s3ConfigurationBuilder().build())
                .dualstackEnabled(config.getValue().dualstack());
        return new RuntimeValue<>(builder);
    }

    private void configureS3Client(S3BaseClientBuilder builder) {
        builder
                .serviceConfiguration(s3ConfigurationBuilder().build())
                .dualstackEnabled(config.getValue().dualstack());
    }

    private S3Configuration.Builder s3ConfigurationBuilder() {
        S3Configuration.Builder s3ConfigBuilder = S3Configuration.builder()
                .accelerateModeEnabled(config.getValue().accelerateMode())
                .checksumValidationEnabled(config.getValue().checksumValidation())
                .chunkedEncodingEnabled(config.getValue().chunkedEncoding())
                .pathStyleAccessEnabled(config.getValue().pathStyleAccess())
                .useArnRegionEnabled(config.getValue().useArnRegionEnabled());
        config.getValue().profileName().ifPresent(s3ConfigBuilder::profileName);
        return s3ConfigBuilder;
    }
}
