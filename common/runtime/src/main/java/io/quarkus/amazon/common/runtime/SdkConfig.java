package io.quarkus.amazon.common.runtime;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.configuration.DurationConverter;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

/**
 * AWS SDK specific configurations
 */
@ConfigGroup
public interface SdkConfig {
    /**
     * The endpoint URI with which the SDK should communicate.
     * <p>
     * If not specified, an appropriate endpoint to be used for the given service and region.
     */
    Optional<URI> endpointOverride();

    /**
     * The amount of time to allow the client to complete the execution of an API call.
     * <p>
     * This timeout covers the entire client execution except for marshalling. This includes request handler execution, all HTTP
     * requests including retries, unmarshalling, etc.
     * <p>
     * This value should always be positive, if present.
     *
     * @see software.amazon.awssdk.core.client.config.ClientOverrideConfiguration#apiCallTimeout()
     **/
    @WithConverter(DurationConverter.class)
    Optional<Duration> apiCallTimeout();

    /**
     * The amount of time to wait for the HTTP request to complete before giving up and timing out.
     * <p>
     * This value should always be positive, if present.
     *
     * @see software.amazon.awssdk.core.client.config.ClientOverrideConfiguration#apiCallAttemptTimeout()
     */
    @WithConverter(DurationConverter.class)
    Optional<Duration> apiCallAttemptTimeout();

    /**
     * sdk client advanced options
     */
    Advanced advanced();

    @ConfigGroup
    public interface Advanced {

        /**
         * Whether the Quarkus thread pool should be used for scheduling tasks such as async retry attempts and timeout task.
         * <p>
         * When disabled, the default sdk behavior is to create a dedicated thread pool for each client, resulting in
         * competition for CPU resources among these thread pools.
         */
        @WithDefault("true")
        boolean useQuarkusScheduledExecutorService();
    }
}
