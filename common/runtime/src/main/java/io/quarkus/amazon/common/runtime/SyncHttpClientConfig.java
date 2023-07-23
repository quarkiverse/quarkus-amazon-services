package io.quarkus.amazon.common.runtime;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.configuration.DurationConverter;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface SyncHttpClientConfig {
    /**
     * The maximum amount of time to establish a connection before timing out.
     */
    @WithDefault("2S")
    @WithConverter(DurationConverter.class)
    Duration connectionTimeout();

    /**
     * The amount of time to wait for data to be transferred over an established, open connection before the connection is timed
     * out.
     */
    @WithDefault("30S")
    @WithConverter(DurationConverter.class)
    Duration socketTimeout();

    /**
     * TLS Key Managers provider configuration
     */
    TlsKeyManagersProviderConfig tlsKeyManagersProvider();

    /**
     * TLS Trust Managers provider configuration
     */
    TlsTrustManagersProviderConfig tlsTrustManagersProvider();

    /**
     * Apache HTTP client specific configurations
     */
    @ConfigDocSection
    ApacheHttpClientConfig apache();

    @ConfigGroup
    public interface ApacheHttpClientConfig {
        /**
         * The amount of time to wait when acquiring a connection from the pool before giving up and timing out.
         */
        @WithDefault("10S")
        @WithConverter(DurationConverter.class)
        Duration connectionAcquisitionTimeout();

        /**
         * The maximum amount of time that a connection should be allowed to remain open while idle.
         */
        @WithDefault("60S")
        @WithConverter(DurationConverter.class)
        Duration connectionMaxIdleTime();

        /**
         * The maximum amount of time that a connection should be allowed to remain open, regardless of usage frequency.
         */
        @WithConverter(DurationConverter.class)
        Optional<Duration> connectionTimeToLive();

        /**
         * The maximum number of connections allowed in the connection pool.
         * <p>
         * Each built HTTP client has its own private connection pool.
         */
        @WithDefault("50")
        int maxConnections();

        /**
         * Whether the client should send an HTTP expect-continue handshake before each request.
         */
        @WithDefault("true")
        boolean expectContinueEnabled();

        /**
         * Whether the idle connections in the connection pool should be closed asynchronously.
         * <p>
         * When enabled, connections left idling for longer than `quarkus.<amazon-service>.sync-client.connection-max-idle-time`
         * will be closed.
         * This will not close connections currently in use.
         */
        @WithDefault("true")
        boolean useIdleConnectionReaper();

        /**
         * Configure whether to enable or disable TCP KeepAlive.
         */
        @WithDefault("false")
        Boolean tcpKeepAlive();

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
             * Currently, the endpoint is limited to a host and port. Any other URI components will result in an exception being
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

            /**
             * For NTLM proxies - the Windows domain name to use when authenticating with the proxy.
             */
            Optional<String> ntlmDomain();

            /**
             * For NTLM proxies - the Windows workstation name to use when authenticating with the proxy.
             */
            Optional<String> ntlmWorkstation();

            /**
             * Whether to attempt to authenticate preemptively against the proxy server using basic authentication.
             */
            Optional<Boolean> preemptiveBasicAuthenticationEnabled();

            /**
             * The hosts that the client is allowed to access without going through the proxy.
             */
            Optional<List<String>> nonProxyHosts();
        }
    }
}
