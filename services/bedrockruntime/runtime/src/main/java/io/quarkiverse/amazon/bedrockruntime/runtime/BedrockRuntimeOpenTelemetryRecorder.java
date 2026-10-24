package io.quarkiverse.amazon.bedrockruntime.runtime;

import java.net.URI;
import java.util.function.Function;

import io.opentelemetry.instrumentation.awssdk.v2_2.AwsSdkTelemetry;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.client.config.ClientAsyncConfiguration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClientBuilder;

@Recorder
public class BedrockRuntimeOpenTelemetryRecorder {

    private final class TelemetryEnabledBedrockRuntimeAsyncClientBuilder
            implements BedrockRuntimeAsyncClientBuilder {

        private BedrockRuntimeAsyncClientBuilder baseBuilder;
        private AwsSdkTelemetry awsSdkTelemetry;

        public TelemetryEnabledBedrockRuntimeAsyncClientBuilder(BedrockRuntimeAsyncClientBuilder baseBuilder,
                AwsSdkTelemetry awsSdkTelemetry) {
            this.baseBuilder = baseBuilder;
            this.awsSdkTelemetry = awsSdkTelemetry;
        }

        @Override
        public BedrockRuntimeAsyncClientBuilder asyncConfiguration(ClientAsyncConfiguration clientAsyncConfiguration) {
            baseBuilder.asyncConfiguration(clientAsyncConfiguration);
            return this;
        }

        @Override
        public BedrockRuntimeAsyncClientBuilder httpClient(SdkAsyncHttpClient httpClient) {
            baseBuilder.httpClient(httpClient);
            return this;
        }

        @Override
        public BedrockRuntimeAsyncClientBuilder httpClientBuilder(
                software.amazon.awssdk.http.async.SdkAsyncHttpClient.Builder httpClientBuilder) {
            baseBuilder.httpClientBuilder(httpClientBuilder);
            return this;
        }

        @Override
        public BedrockRuntimeAsyncClientBuilder region(Region region) {
            baseBuilder.region(region);
            return this;
        }

        @Override
        public BedrockRuntimeAsyncClientBuilder dualstackEnabled(Boolean dualstackEndpointEnabled) {
            baseBuilder.dualstackEnabled(dualstackEndpointEnabled);
            return this;
        }

        @Override
        public BedrockRuntimeAsyncClientBuilder fipsEnabled(Boolean fipsEndpointEnabled) {
            baseBuilder.fipsEnabled(fipsEndpointEnabled);
            return this;
        }

        @Override
        public BedrockRuntimeAsyncClientBuilder overrideConfiguration(ClientOverrideConfiguration overrideConfiguration) {
            baseBuilder.overrideConfiguration(overrideConfiguration);
            return this;
        }

        @Override
        public ClientOverrideConfiguration overrideConfiguration() {
            return baseBuilder.overrideConfiguration();
        }

        @Override
        public BedrockRuntimeAsyncClientBuilder endpointOverride(URI endpointOverride) {
            baseBuilder.endpointOverride(endpointOverride);
            return this;
        }

        @Override
        public BedrockRuntimeAsyncClientBuilder credentialsProvider(
                IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider) {
            baseBuilder.credentialsProvider(credentialsProvider);
            return this;
        }

        @Override
        public BedrockRuntimeAsyncClient build() {
            return awsSdkTelemetry.wrapBedrockRuntimeClient(baseBuilder.build());
        }
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

    protected AwsClientBuilder wrapAsyncClientBuilder(AwsClientBuilder clientBuilder,
            AwsSdkTelemetry awsSdkTelemetry) {

        return (AwsClientBuilder) new TelemetryEnabledBedrockRuntimeAsyncClientBuilder(
                (BedrockRuntimeAsyncClientBuilder) clientBuilder,
                awsSdkTelemetry);
    }
}
