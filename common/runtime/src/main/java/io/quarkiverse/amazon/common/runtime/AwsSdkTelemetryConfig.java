package io.quarkiverse.amazon.common.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.otel.instrumentation")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface AwsSdkTelemetryConfig {

    /**
     * Messaging configuration
     */
    MessagingConfig messaging();

    /**
     * AWS SDK configuration
     */
    AwsSdkConfig awsSdk();

    @ConfigGroup
    public interface AwsSdkConfig {
        /**
         * Sets whether experimental attributes should be set to spans.
         *
         * These attributes may be changed or removed in the future, so only enable this
         * if
         * you know you do not require attributes filled by this instrumentation to be
         * stable across versions.
         */
        @WithDefault("false")
        Optional<Boolean> experimentalSpanAttributes();

        /**
         * Sets whether the io.opentelemetry.context.propagation.TextMapPropagator
         * configured in the provided OpenTelemetry should be used to inject into
         * supported messaging attributes (currently only SQS; SNS may follow).
         *
         * In addition, the X-Ray propagator is always used.
         *
         * Using the messaging propagator is needed if your tracing vendor requires
         * special tracestate entries or legacy propagation information that cannot be
         * transported via X-Ray headers. It may also be useful if you need to directly
         * connect spans over messaging in your tracing backend, bypassing any
         * intermediate spans/X-Ray segments that AWS may create in the delivery
         * process.
         *
         * This option is off by default. If enabled, on extraction the configured
         * propagator will be preferred over X-Ray if it can extract anything.
         */
        @WithDefault("false")
        Optional<Boolean> experimentalUsePropagatorForMessaging();

        /**
         * Sets whether errors returned by each individual HTTP request should be
         * recorded as events for the SDK span.
         *
         * This option is off by default. If enabled, the HTTP error code and the error
         * message will be captured and associated with the span. This provides detailed
         * insights into errors on a per-request basis.
         */
        @WithDefault("false")
        Optional<Boolean> experimentalRecordIndividualHttpError();
    }

    @ConfigGroup
    public interface MessagingConfig {

        /**
         * Experimental configuration
         */
        ExperimentalConfig experimental();

        @ConfigGroup
        public interface ExperimentalConfig {
            /**
             * Configures the messaging headers that will be captured as span attributes.
             */
            Optional<List<String>> captureHeaders();

            /**
             * Set whether to capture the consumer message receive telemetry in messaging
             * instrumentation.
             *
             * Note that this will cause the consumer side to start a new trace, with only a
             * span link connecting it to the producer trace.
             */
            @WithName("receive-telemetry.enabled")
            @WithDefault("false")
            Optional<Boolean> receiveTelemetryEnabled();
        }
    }
}
