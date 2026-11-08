package io.quarkiverse.amazon.common.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sun.net.httpserver.HttpServer;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.WithParentName;
import io.smallrye.config.common.MapBackedConfigSource;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;

class AwsConfigSourceClientTest {
    interface TestBuilder extends AwsClientBuilder<TestBuilder, Object>, AwsSyncClientBuilder<TestBuilder, Object> {
    }

    interface BootstrapConfig {
        @WithParentName
        AmazonClientConfig client();

        SyncHttpClientConfig syncClient();
    }

    @Test
    void bootstrapDoesNotLoadOptionalTransportsRegardlessOfCdiTransportSelection() throws Exception {
        var loader = new ClassLoader(getClass().getClassLoader()) {
            protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("io.quarkiverse.amazon.common.runtime.AmazonClientApache")
                        || name.equals("io.quarkiverse.amazon.common.runtime.AmazonClientAwsCrtTransportRecorder")
                        || name.startsWith("software.amazon.awssdk.http.apache")
                        || name.startsWith("software.amazon.awssdk.http.crt")) {
                    throw new ClassNotFoundException(name);
                }
                if (name.startsWith(AwsConfigSourceClient.class.getName())) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded != null) {
                        return loaded;
                    }
                    try (var input = getResourceAsStream(name.replace('.', '/') + ".class")) {
                        byte[] bytes = input.readAllBytes();
                        return defineClass(name, bytes, 0, bytes.length);
                    } catch (IOException failure) {
                        throw new ClassNotFoundException(name, failure);
                    }
                }
                return super.loadClass(name, resolve);
            }
        };
        var configure = Class.forName(AwsConfigSourceClient.class.getName(), true, loader).getMethod("configure",
                AwsClientBuilder.class, String.class, AmazonClientConfig.class, SyncHttpClientConfig.class,
                ConfigSourceContext.class);
        var builder = (TestBuilder) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { TestBuilder.class },
                (proxy, method, args) -> proxy);
        for (String type : List.of("url", "apache", "apache5", "aws-crt")) {
            var config = new SmallRyeConfigBuilder().withMapping(BootstrapConfig.class, "quarkus.bootstrap")
                    .withDefaultValue("quarkus.bootstrap.aws.region", "us-east-1")
                    .withDefaultValue("quarkus.bootstrap.aws.credentials.type", "anonymous")
                    .withDefaultValue("quarkus.bootstrap.sync-client.type", type).withValidateUnknown(false).build();
            var mapping = config.getConfigMapping(BootstrapConfig.class, "quarkus.bootstrap");
            configure.invoke(null, builder, "bootstrap", mapping.client(), mapping.syncClient(), context(config));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "url", "apache", "apache5", "aws-crt" })
    void appliesSdkTimeoutsAndAlwaysUsesUrlConnection(String type) {
        var captured = configure(Map.of("quarkus.bootstrap.sync-client.type", type,
                "quarkus.bootstrap.api-call-timeout", "3S", "quarkus.bootstrap.api-call-attempt-timeout", "1S"));
        var overrides = (ClientOverrideConfiguration) captured.get("overrideConfiguration");
        assertEquals(Duration.ofSeconds(3), overrides.apiCallTimeout().orElseThrow());
        assertEquals(Duration.ofSeconds(1), overrides.apiCallAttemptTimeout().orElseThrow());
        assertTrue(overrides.scheduledExecutorService().isEmpty());
        assertTrue(overrides.retryStrategy().isEmpty());
        try (var http = http(captured)) {
            assertEquals("UrlConnectionHttpClient", http.getClass().getSimpleName());
        }
    }

    @Test
    void socketTimeoutIsUsedByTheActualTransport() throws Exception {
        var release = new CountDownLatch(1);
        var received = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(executor);
        server.createContext("/", exchange -> {
            received.countDown();
            try {
                assertTrue(release.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
        try (var http = http(configure(Map.of("quarkus.bootstrap.sync-client.socket-timeout", "100ms")))) {
            assertTimeoutPreemptively(Duration.ofSeconds(3), () -> assertThrows(IOException.class,
                    () -> http.prepareRequest(request(URI.create("http://127.0.0.1:" + server.getAddress().getPort())))
                            .call()));
            assertTrue(received.await(1, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            server.stop(0);
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void bootstrapRejectsCdiCredentialsWithAnActionableError() {
        var failure = assertThrows(RuntimeConfigurationError.class,
                () -> configure(Map.of("quarkus.bootstrap.aws.credentials.type", "custom")));
        assertTrue(failure.getMessage().contains("requires CDI"));
        assertNull(failure.getCause());
    }

    @Test
    void transportValidationCannotExposeTlsConfiguration() {
        var failure = assertThrows(RuntimeConfigurationError.class, () -> configure(Map.of(
                "quarkus.bootstrap.sync-client.tls-trust-managers-provider.type", "file-store",
                "quarkus.bootstrap.sync-client.tls-trust-managers-provider.file-store.path", "unsafe-secret-store",
                "quarkus.bootstrap.sync-client.tls-trust-managers-provider.file-store.password", "unsafe-password")));
        assertEquals(
                "Unable to configure quarkus.bootstrap bootstrap client [RuntimeConfigurationError]; check AWS, API timeout and sync-client settings.",
                failure.getMessage());
        assertNull(failure.getCause());
    }

    @Test
    void contextProfilesArePreservedWhenMappingBootstrapSettings() {
        var config = new SmallRyeConfigBuilder().withProfiles(List.of("first", "second"))
                .withSources(new MapBackedConfigSource("test", Map.of(
                        "%first.quarkus.bootstrap.api-call-timeout", "1S",
                        "%second.quarkus.bootstrap.api-call-timeout", "2S")) {
                }).build();
        var timeout = AwsConfigSourceClient.configBuilder(context(config))
                .withMapping(BootstrapConfig.class, "quarkus.bootstrap")
                .build().getConfigMapping(BootstrapConfig.class, "quarkus.bootstrap").client().sdk().apiCallTimeout();
        assertEquals(Duration.ofSeconds(2), timeout.orElseThrow());
    }

    private static HttpExecuteRequest request(URI uri) {
        return HttpExecuteRequest.builder().request(SdkHttpFullRequest.builder().uri(uri).method(SdkHttpMethod.GET).build())
                .build();
    }

    private static SdkHttpClient http(Map<String, Object> captured) {
        return ((SdkHttpClient.Builder<?>) captured.get("httpClientBuilder")).build();
    }

    private static Map<String, Object> configure(Map<String, String> properties) {
        var config = new SmallRyeConfigBuilder().withMapping(BootstrapConfig.class, "quarkus.bootstrap")
                .withDefaultValue("quarkus.bootstrap.aws.region", "us-east-1")
                .withDefaultValue("quarkus.bootstrap.aws.credentials.type", "anonymous")
                .withSources(new MapBackedConfigSource("test", properties) {
                }).withValidateUnknown(false).build();
        var mapping = config.getConfigMapping(BootstrapConfig.class, "quarkus.bootstrap");
        var captured = new HashMap<String, Object>();
        var builder = (TestBuilder) Proxy.newProxyInstance(AwsConfigSourceClientTest.class.getClassLoader(),
                new Class<?>[] { TestBuilder.class }, (proxy, method, args) -> {
                    if (method.getName().equals("overrideConfiguration")) {
                        @SuppressWarnings("unchecked")
                        var consumer = (Consumer<ClientOverrideConfiguration.Builder>) args[0];
                        var overrides = ClientOverrideConfiguration.builder();
                        consumer.accept(overrides);
                        captured.put(method.getName(), overrides.build());
                    } else if (args != null && args.length == 1) {
                        captured.put(method.getName(), args[0]);
                    }
                    return proxy;
                });
        AwsConfigSourceClient.configure(builder, "bootstrap", mapping.client(), mapping.syncClient(), context(config));
        return captured;
    }

    private static ConfigSourceContext context(SmallRyeConfig config) {
        return new ConfigSourceContext() {
            public ConfigValue getValue(String name) {
                return config.getConfigValue(name);
            }

            public Iterator<String> iterateNames() {
                return config.getPropertyNames().iterator();
            }

            public List<String> getProfiles() {
                return config.getProfiles();
            }

            public List<ConfigSource> getConfigSources() {
                var sources = new ArrayList<ConfigSource>();
                config.getConfigSources().forEach(sources::add);
                return sources;
            }
        };
    }
}
