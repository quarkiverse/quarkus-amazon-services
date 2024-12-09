package io.quarkiverse.amazon.common.runtime;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.configuration.DurationConverter;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface CrtHttpClientConfig {

    /**
     * The maximum amount of time that a connection should be allowed to remain open
     * while idle.
     */
    @ConfigDocDefault("60S")
    @WithConverter(DurationConverter.class)
    Optional<Duration> connectionMaxIdleTime();

    /**
     * The maximum number of allowed concurrent requests.
     */
    @ConfigDocDefault("50")
    Optional<Integer> maxConcurrency();

    /**
     * HTTP proxy configuration
     */
    HttpClientProxyConfiguration proxy();

    @ConfigGroup
    public interface HttpClientProxyConfiguration {

        /**
         * Enable HTTP proxy
         */
        @WithDefault("false")
        boolean enabled();

        /**
         * The endpoint of the proxy server that the SDK should connect through.
         * <p>
         * Currently, the endpoint is limited to a host and port. Any other URI
         * components will result in an exception being
         * raised.
         */
        Optional<URI> endpoint();

        /**
         * The username to use when connecting through a proxy.
         */
        Optional<String> username();

        /**
         * The password to use when connecting through a proxy.
         */
        Optional<String> password();
    }
}