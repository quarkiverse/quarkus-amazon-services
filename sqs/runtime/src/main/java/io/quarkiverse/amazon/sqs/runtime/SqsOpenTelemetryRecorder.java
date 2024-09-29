package io.quarkiverse.amazon.sqs.runtime;

import java.net.URI;

import io.opentelemetry.instrumentation.awssdk.v2_2.AwsSdkTelemetry;
import io.quarkiverse.amazon.common.runtime.AmazonClientOpenTelemetryRecorder;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.client.config.ClientAsyncConfiguration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClient.Builder;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

@Recorder
public class SqsOpenTelemetryRecorder extends AmazonClientOpenTelemetryRecorder {

    private final class TelemetryEnabledSqsSyncClientBuilder
            implements SqsClientBuilder {

        private SqsClientBuilder baseBuilder;
        private AwsSdkTelemetry awsSdkTelemetry;

        public TelemetryEnabledSqsSyncClientBuilder(SqsClientBuilder baseBuilder, AwsSdkTelemetry awsSdkTelemetry) {
            this.baseBuilder = baseBuilder;
            this.awsSdkTelemetry = awsSdkTelemetry;
        }

        @Override
        public SqsClientBuilder httpClient(SdkHttpClient httpClient) {
            return baseBuilder.httpClient(httpClient);
        }

        @Override
        public SqsClientBuilder httpClientBuilder(Builder httpClientBuilder) {
            return baseBuilder.httpClientBuilder(httpClientBuilder);
        }

        @Override
        public SqsClientBuilder region(Region region) {
            return baseBuilder.region(region);
        }

        @Override
        public SqsClientBuilder checksumValidationEnabled(Boolean checksumValidationEnabled) {
            return baseBuilder.checksumValidationEnabled(checksumValidationEnabled);
        }

        @Override
        public SqsClientBuilder dualstackEnabled(Boolean dualstackEndpointEnabled) {
            return baseBuilder.dualstackEnabled(dualstackEndpointEnabled);
        }

        @Override
        public SqsClientBuilder fipsEnabled(Boolean fipsEndpointEnabled) {
            return baseBuilder.fipsEnabled(fipsEndpointEnabled);
        }

        @Override
        public SqsClientBuilder overrideConfiguration(ClientOverrideConfiguration overrideConfiguration) {
            return baseBuilder.overrideConfiguration(overrideConfiguration);
        }

        @Override
        public ClientOverrideConfiguration overrideConfiguration() {
            return baseBuilder.overrideConfiguration();
        }

        @Override
        public SqsClientBuilder endpointOverride(URI endpointOverride) {
            return baseBuilder.endpointOverride(endpointOverride);
        }

        @Override
        public SqsClient build() {
            return awsSdkTelemetry.wrap(baseBuilder.build());
        }
    }

    private final class TelemetryEnabledSqsAsyncClientBuilder
            implements SqsAsyncClientBuilder {

        private SqsAsyncClientBuilder baseBuilder;
        private AwsSdkTelemetry awsSdkTelemetry;

        public TelemetryEnabledSqsAsyncClientBuilder(SqsAsyncClientBuilder baseBuilder,
                AwsSdkTelemetry awsSdkTelemetry) {
            this.baseBuilder = baseBuilder;
            this.awsSdkTelemetry = awsSdkTelemetry;
        }

        @Override
        public SqsAsyncClientBuilder asyncConfiguration(ClientAsyncConfiguration clientAsyncConfiguration) {
            return baseBuilder.asyncConfiguration(clientAsyncConfiguration);
        }

        @Override
        public SqsAsyncClientBuilder httpClient(SdkAsyncHttpClient httpClient) {
            return baseBuilder.httpClient(httpClient);
        }

        @Override
        public SqsAsyncClientBuilder httpClientBuilder(
                software.amazon.awssdk.http.async.SdkAsyncHttpClient.Builder httpClientBuilder) {
            return baseBuilder.httpClientBuilder(httpClientBuilder);
        }

        @Override
        public SqsAsyncClientBuilder checksumValidationEnabled(Boolean checksumValidationEnabled) {
            return baseBuilder.checksumValidationEnabled(checksumValidationEnabled);
        }

        @Override
        public SqsAsyncClientBuilder region(Region region) {
            return baseBuilder.region(region);
        }

        @Override
        public SqsAsyncClientBuilder dualstackEnabled(Boolean dualstackEndpointEnabled) {
            return baseBuilder.dualstackEnabled(dualstackEndpointEnabled);
        }

        @Override
        public SqsAsyncClientBuilder fipsEnabled(Boolean fipsEndpointEnabled) {
            return baseBuilder.fipsEnabled(fipsEndpointEnabled);
        }

        @Override
        public SqsAsyncClientBuilder overrideConfiguration(ClientOverrideConfiguration overrideConfiguration) {
            return baseBuilder.overrideConfiguration(overrideConfiguration);
        }

        @Override
        public ClientOverrideConfiguration overrideConfiguration() {
            return baseBuilder.overrideConfiguration();
        }

        @Override
        public SqsAsyncClientBuilder endpointOverride(URI endpointOverride) {
            return baseBuilder.endpointOverride(endpointOverride);
        }

        @Override
        public SqsAsyncClient build() {
            return awsSdkTelemetry.wrap(baseBuilder.build());
        }
    }

    public SqsOpenTelemetryRecorder() {
    }

    @Override
    protected AwsClientBuilder wrapSyncClientBuilder(AwsClientBuilder clientBuilder,
            RuntimeValue<AwsSdkTelemetry> awsSdkTelemetry) {

        return (AwsClientBuilder) new TelemetryEnabledSqsSyncClientBuilder((SqsClientBuilder) clientBuilder,
                awsSdkTelemetry.getValue());
    }

    @Override
    protected AwsClientBuilder wrapAsyncClientBuilder(AwsClientBuilder clientBuilder,
            RuntimeValue<AwsSdkTelemetry> awsSdkTelemetry) {

        return (AwsClientBuilder) new TelemetryEnabledSqsAsyncClientBuilder((SqsAsyncClientBuilder) clientBuilder,
                awsSdkTelemetry.getValue());
    }
}
