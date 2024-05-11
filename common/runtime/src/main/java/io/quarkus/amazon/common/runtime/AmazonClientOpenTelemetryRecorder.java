package io.quarkus.amazon.common.runtime;

import java.util.function.Function;

import io.opentelemetry.instrumentation.awssdk.v2_2.AwsSdkTelemetry;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;

@Recorder
public class AmazonClientOpenTelemetryRecorder {

    public Function<SyntheticCreationalContext<AwsClientBuilder>, AwsClientBuilder> configureSync(
            RuntimeValue<AwsClientBuilder> clientBuilder) {
        return new Function<SyntheticCreationalContext<AwsClientBuilder>, AwsClientBuilder>() {
            @Override
            public AwsClientBuilder apply(SyntheticCreationalContext<AwsClientBuilder> context) {
                AwsClientBuilder builder = clientBuilder.getValue();
                AwsSdkTelemetry awsSdkTelemetry = context.getInjectedReference(AwsSdkTelemetry.class);

                builder.overrideConfiguration(
                        builder.overrideConfiguration().toBuilder()
                                .addExecutionInterceptor(awsSdkTelemetry.newExecutionInterceptor())
                                .build());

                return wrapSyncClientBuilder(builder, new RuntimeValue<>(awsSdkTelemetry));
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
                                .addExecutionInterceptor(awsSdkTelemetry.newExecutionInterceptor())
                                .build());

                return wrapAsyncClientBuilder(builder, new RuntimeValue<>(awsSdkTelemetry));
            }
        };
    }

    protected AwsClientBuilder wrapSyncClientBuilder(AwsClientBuilder clientBuilder,
            RuntimeValue<AwsSdkTelemetry> awsSdkTelemetry) {
        return clientBuilder;
    };

    protected AwsClientBuilder wrapAsyncClientBuilder(AwsClientBuilder clientBuilder,
            RuntimeValue<AwsSdkTelemetry> awsSdkTelemetry) {
        return clientBuilder;
    };
}
