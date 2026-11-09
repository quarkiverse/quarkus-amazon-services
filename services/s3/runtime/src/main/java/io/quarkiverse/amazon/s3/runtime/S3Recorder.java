package io.quarkiverse.amazon.s3.runtime;

import io.quarkiverse.amazon.common.runtime.AmazonClientRecorder;
import io.quarkiverse.amazon.common.runtime.AsyncHttpClientConfig;
import io.quarkiverse.amazon.common.runtime.ClientUtil;
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
        return getSyncBuilder(ClientUtil.DEFAULT_CLIENT_NAME).getValue();
    }

    @Override
    public RuntimeValue<AwsSyncClientBuilder<?, ?>> getSyncBuilder(String clientName) {
        S3ClientBuilder builder = S3Client.builder();
        configureS3Client(builder, clientName);
        return new RuntimeValue<>(builder);
    }

    @Override
    public AwsAsyncClientBuilder<?, ?> getAsyncClientBuilder() {
        return getAsyncBuilder(ClientUtil.DEFAULT_CLIENT_NAME).getValue();
    }

    @Override
    public RuntimeValue<AwsAsyncClientBuilder<?, ?>> getAsyncBuilder(String clientName) {
        S3AsyncClientBuilder builder = S3AsyncClient.builder();
        configureS3Client(builder, clientName);

        S3Config.MultipartConfig multipart = config.getValue().s3ConfigFor(clientName).multipart();
        S3Config.MultipartConfig defaults = config.getValue().s3ConfigFor(ClientUtil.DEFAULT_CLIENT_NAME).multipart();
        if (multipart.enabled().or(defaults::enabled).orElse(false)) {
            builder.multipartEnabled(true);
            MultipartConfiguration.Builder multipartBuilder = MultipartConfiguration.builder();
            String prefix = config.getValue().configPrefix("quarkus.s3", clientName) + ".multipart.";
            multipart.thresholdInBytes().or(defaults::thresholdInBytes).ifPresent(value -> multipartBuilder
                    .thresholdInBytes(Validate.isPositive(value.asLongValue(), prefix + "threshold-in-bytes")));
            multipart.minimumPartSizeInBytes().or(defaults::minimumPartSizeInBytes).ifPresent(value -> multipartBuilder
                    .minimumPartSizeInBytes(
                            Validate.isPositive(value.asLongValue(), prefix + "minimum-part-size-in-bytes")));
            builder.multipartConfiguration(multipartBuilder.build());
        }

        return new RuntimeValue<>(builder);
    }

    @Override
    public RuntimeValue<SdkPresigner.Builder> createPresignerBuilder() {
        return createPresignerBuilder(ClientUtil.DEFAULT_CLIENT_NAME);
    }

    @Override
    public RuntimeValue<SdkPresigner.Builder> createPresignerBuilder(String clientName) {
        S3Presigner.Builder builder = S3Presigner.builder()
                .serviceConfiguration(s3ConfigurationBuilder(clientName).build())
                .dualstackEnabled(dualstack(clientName));
        return new RuntimeValue<>(builder);
    }

    private void configureS3Client(S3BaseClientBuilder builder, String clientName) {
        builder
                .serviceConfiguration(s3ConfigurationBuilder(clientName).build())
                .dualstackEnabled(dualstack(clientName));
    }

    private boolean dualstack(String clientName) {
        return config.getValue().s3ConfigFor(clientName).dualstack()
                .or(config.getValue().s3ConfigFor(ClientUtil.DEFAULT_CLIENT_NAME)::dualstack).orElse(false);
    }

    private S3Configuration.Builder s3ConfigurationBuilder(String clientName) {
        S3Config.S3ClientConfig client = config.getValue().s3ConfigFor(clientName);
        S3Config.S3ClientConfig defaults = config.getValue().s3ConfigFor(ClientUtil.DEFAULT_CLIENT_NAME);
        S3Configuration.Builder s3ConfigBuilder = S3Configuration.builder()
                .accelerateModeEnabled(client.accelerateMode().or(defaults::accelerateMode).orElse(false))
                .checksumValidationEnabled(client.checksumValidation().or(defaults::checksumValidation).orElse(true))
                .chunkedEncodingEnabled(client.chunkedEncoding().or(defaults::chunkedEncoding).orElse(true))
                .pathStyleAccessEnabled(client.pathStyleAccess().or(defaults::pathStyleAccess).orElse(false))
                .useArnRegionEnabled(client.useArnRegionEnabled().or(defaults::useArnRegionEnabled).orElse(false));
        client.profileName().or(defaults::profileName).ifPresent(s3ConfigBuilder::profileName);
        return s3ConfigBuilder;
    }
}
