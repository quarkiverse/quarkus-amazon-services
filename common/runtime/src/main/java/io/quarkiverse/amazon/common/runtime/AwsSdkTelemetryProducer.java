package io.quarkiverse.amazon.common.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.awssdk.v2_2.AwsSdkTelemetry;
import io.quarkus.arc.DefaultBean;

@ApplicationScoped
public class AwsSdkTelemetryProducer {

    @DefaultBean
    @Produces
    @Singleton
    public AwsSdkTelemetry defaultAwsSdkTelemetry(OpenTelemetry openTelemetry, AwsSdkTelemetryConfig config) {
        var builder = AwsSdkTelemetry.builder(openTelemetry);

        config.messaging().experimental().captureHeaders().ifPresent(builder::setCapturedHeaders);
        config.messaging().experimental().receiveTelemetryEnabled()
                .ifPresent(builder::setMessagingReceiveInstrumentationEnabled);
        config.awsSdk().experimentalUsePropagatorForMessaging().ifPresent(builder::setUseConfiguredPropagatorForMessaging);
        config.awsSdk().experimentalRecordIndividualHttpError().ifPresent(builder::setRecordIndividualHttpError);
        config.awsSdk().experimentalSpanAttributes().ifPresent(builder::setCaptureExperimentalSpanAttributes);

        return builder.build();
    }
}
