package io.quarkiverse.amazon.sqs.runtime;

import java.net.URI;
import java.util.function.Function;

import io.opentelemetry.instrumentation.awssdk.v2_2.AwsSdkTelemetry;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.client.config.ClientAsyncConfiguration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClient.Builder;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

@Recorder
public class SqsOpenTelemetryRecorder {

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
            baseBuilder.httpClient(httpClient);
            return this;
        }

        @Override
        public SqsClientBuilder httpClientBuilder(Builder httpClientBuilder) {
            baseBuilder.httpClientBuilder(httpClientBuilder);
            return this;
        }

        @Override
        public SqsClientBuilder region(Region region) {
            baseBuilder.region(region);
            return this;
        }

        @Override
        public SqsClientBuilder checksumValidationEnabled(Boolean checksumValidationEnabled) {
            baseBuilder.checksumValidationEnabled(checksumValidationEnabled);
            return this;
        }

        @Override
        public SqsClientBuilder dualstackEnabled(Boolean dualstackEndpointEnabled) {
            baseBuilder.dualstackEnabled(dualstackEndpointEnabled);
            return this;
        }

        @Override
        public SqsClientBuilder fipsEnabled(Boolean fipsEndpointEnabled) {
            baseBuilder.fipsEnabled(fipsEndpointEnabled);
            return this;
        }

        @Override
        public SqsClientBuilder overrideConfiguration(ClientOverrideConfiguration overrideConfiguration) {
            baseBuilder.overrideConfiguration(overrideConfiguration);
            return this;
        }

        @Override
        public ClientOverrideConfiguration overrideConfiguration() {
            return baseBuilder.overrideConfiguration();
        }

        @Override
        public SqsClientBuilder endpointOverride(URI endpointOverride) {
            baseBuilder.endpointOverride(endpointOverride);
            return this;
        }

        @Override
        public SqsClientBuilder credentialsProvider(
                IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider) {
            baseBuilder.credentialsProvider(credentialsProvider);
            return this;
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
            baseBuilder.asyncConfiguration(clientAsyncConfiguration);
            return this;
        }

        @Override
        public SqsAsyncClientBuilder httpClient(SdkAsyncHttpClient httpClient) {
            baseBuilder.httpClient(httpClient);
            return this;
        }

        @Override
        public SqsAsyncClientBuilder httpClientBuilder(
                software.amazon.awssdk.http.async.SdkAsyncHttpClient.Builder httpClientBuilder) {
            baseBuilder.httpClientBuilder(httpClientBuilder);
            return this;
        }

        @Override
        public SqsAsyncClientBuilder checksumValidationEnabled(Boolean checksumValidationEnabled) {
            baseBuilder.checksumValidationEnabled(checksumValidationEnabled);
            return this;
        }

        @Override
        public SqsAsyncClientBuilder region(Region region) {
            baseBuilder.region(region);
            return this;
        }

        @Override
        public SqsAsyncClientBuilder dualstackEnabled(Boolean dualstackEndpointEnabled) {
            baseBuilder.dualstackEnabled(dualstackEndpointEnabled);
            return this;
        }

        @Override
        public SqsAsyncClientBuilder fipsEnabled(Boolean fipsEndpointEnabled) {
            baseBuilder.fipsEnabled(fipsEndpointEnabled);
            return this;
        }

        @Override
        public SqsAsyncClientBuilder overrideConfiguration(ClientOverrideConfiguration overrideConfiguration) {
            baseBuilder.overrideConfiguration(overrideConfiguration);
            return this;
        }

        @Override
        public ClientOverrideConfiguration overrideConfiguration() {
            return baseBuilder.overrideConfiguration();
        }

        @Override
        public SqsAsyncClientBuilder endpointOverride(URI endpointOverride) {
            baseBuilder.endpointOverride(endpointOverride);
            return this;
        }

        @Override
        public SqsAsyncClientBuilder credentialsProvider(
                IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider) {
            baseBuilder.credentialsProvider(credentialsProvider);
            return this;
        }

        @Override
        public SqsAsyncClient build() {
            return awsSdkTelemetry.wrap(baseBuilder.build());
        }
    }

    public Function<SyntheticCreationalContext<AwsClientBuilder>, AwsClientBuilder> configureSync(
            RuntimeValue<AwsClientBuilder> clientBuilder) {
        return new Function<SyntheticCreationalContext<AwsClientBuilder>, AwsClientBuilder>() {
            @Override
            public AwsClientBuilder apply(SyntheticCreationalContext<AwsClientBuilder> context) {
                AwsClientBuilder builder = clientBuilder.getValue();
                AwsSdkTelemetry awsSdkTelemetry = context.getInjectedReference(AwsSdkTelemetry.class);

                builder.overrideConfiguration(
                        builder.overrideConfiguration().toBuilder()
                                .addExecutionInterceptor(awsSdkTelemetry.createExecutionInterceptor())
                                .build());

                return wrapSyncClientBuilder(builder, awsSdkTelemetry);
            }
        };
    }

    public Function<SyntheticCreationalContext<AwsClientBuilder>, AwsClientBuilder> configureAsync(
            RuntimeValue<AwsClientBuilder> clientBuilder) {
        return new Function<SyntheticCreationalContext<AwsClientBuilder>, AwsClientBuilder>() {
            @Override
            public AwsClientBuilder apply(SyntheticCreationalContext<AwsClientBuilder> context) {
                AwsClientBuilder builder = clientBuilder.getValue();
                AwsSdkTelemetry awsSdkTelemetry = context.getInjectedReference(AwsSdkTelemetry.class);

                builder.overrideConfiguration(
                        builder.overrideConfiguration().toBuilder()
                                .addExecutionInterceptor(awsSdkTelemetry.createExecutionInterceptor())
                                .build());

                return wrapAsyncClientBuilder(builder, awsSdkTelemetry);
            }
        };
    }

    protected AwsClientBuilder wrapSyncClientBuilder(AwsClientBuilder clientBuilder,
            AwsSdkTelemetry awsSdkTelemetry) {

        return (AwsClientBuilder) new TelemetryEnabledSqsSyncClientBuilder((SqsClientBuilder) clientBuilder,
                awsSdkTelemetry);
    }

    protected AwsClientBuilder wrapAsyncClientBuilder(AwsClientBuilder clientBuilder,
            AwsSdkTelemetry awsSdkTelemetry) {

        return (AwsClientBuilder) new TelemetryEnabledSqsAsyncClientBuilder((SqsAsyncClientBuilder) clientBuilder,
                awsSdkTelemetry);
    }
}
