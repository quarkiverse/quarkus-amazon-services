package io.quarkiverse.amazon.common.runtime;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

//import io.netty.handler.ssl.SslProvider;
import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.configuration.DurationConverter;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import software.amazon.awssdk.http.Protocol;

@ConfigGroup
public interface AsyncHttpClientConfig {

    /**
     * The maximum number of allowed concurrent requests.
     * <p>
     * For HTTP/1.1 this is the same as max connections. For HTTP/2 the number of
     * connections that will be used depends on the
     * max streams allowed per connection.
     */
    @WithDefault("50")
    int maxConcurrency();

    /**
     * The maximum number of pending acquires allowed.
     * <p>
     * Once this exceeds, acquire tries will be failed.
     */
    @WithDefault("10000")
    int maxPendingConnectionAcquires();

    /**
     * The amount of time to wait for a read on a socket before an exception is
     * thrown.
     * <p>
     * Specify `0` to disable.
     */
    @WithDefault("30S")
    @WithConverter(DurationConverter.class)
    Duration readTimeout();

    /**
     * The amount of time to wait for a write on a socket before an exception is
     * thrown.
     * <p>
     * Specify `0` to disable.
     */
    @WithDefault("30S")
    @WithConverter(DurationConverter.class)
    Duration writeTimeout();

    /**
     * The amount of time to wait when initially establishing a connection before
     * giving up and timing out.
     */
    @WithDefault("10S")
    @WithConverter(DurationConverter.class)
    Duration connectionTimeout();

    /**
     * The amount of time to wait when acquiring a connection from the pool before
     * giving up and timing out.
     */
    @WithDefault("2S")
    @WithConverter(DurationConverter.class)
    Duration connectionAcquisitionTimeout();

    /**
     * The maximum amount of time that a connection should be allowed to remain
     * open, regardless of usage frequency.
     */
    @WithConverter(DurationConverter.class)
    Optional<Duration> connectionTimeToLive();

    /**
     * The maximum amount of time that a connection should be allowed to remain open
     * while idle.
     * <p>
     * Currently has no effect if
     * `quarkus.<amazon-service>.async-client.use-idle-connection-reaper` is false.
     */
    @WithDefault("5S")
    @WithConverter(DurationConverter.class)
    Duration connectionMaxIdleTime();

    /**
     * Whether the idle connections in the connection pool should be closed.
     * <p>
     * When enabled, connections left idling for longer than
     * `quarkus.<amazon-service>.async-client.connection-max-idle-time`
     * will be closed. This will not close connections currently in use.
     */
    @WithDefault("true")
    boolean useIdleConnectionReaper();

    /**
     * Configure whether to enable or disable TCP KeepAlive.
     */
    @WithDefault("false")
    Boolean tcpKeepAlive();

    /**
     * The HTTP protocol to use.
     */
    @WithDefault("http1-1")
    Protocol protocol();

    /**
     * The SSL Provider to be used in the Netty client.
     * <p>
     * Default is `OPENSSL` if available, `JDK` otherwise.
     */
    Optional<SslProviderType> sslProvider();

    /**
     * HTTP/2 specific configuration
     */
    Http2Config http2();

    /**
     * HTTP proxy configuration
     */
    NettyProxyConfiguration proxy();

    /**
     * TLS Key Managers provider configuration
     */
    TlsKeyManagersProviderConfig tlsKeyManagersProvider();

    /**
     * TLS Trust Managers provider configuration
     */
    TlsTrustManagersProviderConfig tlsTrustManagersProvider();

    /**
     * Netty event loop configuration override
     */
    SdkEventLoopGroupConfig eventLoop();

    /**
     * Async client advanced options
     */
    Advanced advanced();

    @ConfigGroup
    public interface Http2Config {
        /**
         * The maximum number of concurrent streams for an HTTP/2 connection.
         * <p>
         * This setting is only respected when the HTTP/2 protocol is used.
         * <p>
         */
        @ConfigDocDefault("4294967295")
        Optional<Long> maxStreams();

        /**
         * The initial window size for an HTTP/2 stream.
         * <p>
         * This setting is only respected when the HTTP/2 protocol is used.
         * <p>
         */
        @ConfigDocDefault("1048576")
        OptionalInt initialWindowSize();

        /**
         * Sets the period that the Netty client will send {@code PING} frames to the
         * remote endpoint to check the
         * health of the connection. To disable this feature, set a duration of 0.
         * <p>
         * This setting is only respected when the HTTP/2 protocol is used.
         * <p>
         */
        @ConfigDocDefault("5")
        @WithConverter(DurationConverter.class)
        Optional<Duration> healthCheckPingPeriod();
    }

    @ConfigGroup
    public interface SdkEventLoopGroupConfig {

        /**
         * Enable the custom configuration of the Netty event loop group.
         */
        @WithDefault("false")
        boolean override();

        /**
         * Number of threads to use for the event loop group.
         * <p>
         * If not set, the default Netty thread count is used (which is double the
         * number of available processors unless the
         * `io.netty.eventLoopThreads` system property is set.
         */
        OptionalInt numberOfThreads();

        /**
         * The thread name prefix for threads created by this thread factory used by
         * event loop group.
         * <p>
         * The prefix will be appended with a number unique to the thread factory and a
         * number unique to the thread.
         * <p>
         * If not specified it defaults to `aws-java-sdk-NettyEventLoop`
         */
        Optional<String> threadNamePrefix();
    }

    @ConfigGroup
    public interface NettyProxyConfiguration {

        /**
         * Enable HTTP proxy.
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
         * The hosts that the client is allowed to access without going through the
         * proxy.
         */
        Optional<List<String>> nonProxyHosts();
    }

    @ConfigGroup
    public interface Advanced {

        /**
         * Whether the default thread pool should be used to complete the futures
         * returned from the HTTP client request.
         * <p>
         * When disabled, futures will be completed on the Netty event loop thread.
         */
        @WithDefault("true")
        boolean useFutureCompletionThreadPool();
    }

    // TODO: additionalChannelOptions
    // additionalChannelOptions;

    /**
     * An enumeration of SSL/TLS protocol providers.
     */
    public enum SslProviderType {
        JDK,
        OPENSSL,
        OPENSSL_REFCNT
    }
}
