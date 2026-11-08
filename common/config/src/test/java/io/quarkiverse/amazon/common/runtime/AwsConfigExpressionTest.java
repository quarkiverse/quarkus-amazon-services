package io.quarkiverse.amazon.common.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.smallrye.config.PropertiesConfigSourceLoader;
import io.smallrye.config.SecretKeysHandlerFactory.LazySecretKeysHandler;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;

class AwsConfigExpressionTest {
    @ParameterizedTest
    @ValueSource(strings = { "sm", "ssm" })
    void recursiveBootstrapFailsWithoutOverflowAndInitializationCanRecover(String name) {
        String service = name.equals("sm") ? "secretsmanager" : "ssm";
        String bootstrap = "quarkus." + service + ".endpoint-override";
        var recurse = new java.util.concurrent.atomic.AtomicBoolean(true);
        var provider = new Provider(resource -> "resolved");
        try (var handler = new AwsConfigExpression(name, service, context -> {
            if (recurse.get()) {
                context.getValue(bootstrap);
            }
            return provider;
        })) {
            var config = config(handler, Map.of("value", "${" + name + "::secret}",
                    bootstrap, "${" + name + "::bootstrap}"));
            var failure = assertThrows(IllegalStateException.class, () -> config.getValue("value", String.class));
            assertNull(failure.getCause());
            assertFalse(failure.getMessage().contains("bootstrap}"));
            assertEquals(0, provider.calls);
            recurse.set(false);
            assertEquals("resolved", config.getValue("value", String.class));
        }
    }

    @Test
    void recursiveCachePolicyFailsWithoutOverflow() {
        try (var handler = handler("sm", new Provider(resource -> "value"))) {
            var config = config(handler, Map.of("value", "${sm::secret}",
                    "quarkus.secretsmanager.expression.cache-ttl", "${sm::ttl}"));
            assertThrows(IllegalStateException.class, () -> config.getValue("value", String.class));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "sm", "ssm" })
    void profileFilesReachTheLazyHandlerIncludingItsCachePolicy(String name, @TempDir Path directory) throws Exception {
        String service = name.equals("sm") ? "secretsmanager" : "ssm";
        var base = directory.resolve("application.properties");
        Files.writeString(base, "value=${" + name + "::secret}\n");
        Files.writeString(directory.resolve("application-prod.properties"),
                "quarkus." + service + ".aws.region=us-west-2\nquarkus." + service + ".expression.cache-ttl=0S\n");
        var provider = new Provider(resource -> "value");
        try (var handler = new AwsConfigExpression(name, service, context -> {
            assertEquals("us-west-2", context.getValue("quarkus." + service + ".aws.region").getValue());
            return provider;
        })) {
            var config = new SmallRyeConfigBuilder().withProfile("prod")
                    .withSources(PropertiesConfigSourceLoader.inFileSystem(base.toUri().toString(), 250,
                            Thread.currentThread().getContextClassLoader()))
                    .withSources(handler).withSecretKeysHandlers(new LazySecretKeysHandler(handler)).build();
            assertEquals("0S", config.getValue("quarkus." + service + ".expression.cache-ttl", String.class));
            assertEquals("value", config.getValue("value", String.class));
            assertEquals("value", config.getValue("value", String.class));
            assertEquals(2, provider.calls);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "/a~2", "/a~", "/~~01", "/a/01", "/a/+1", "/a/-", "/a/999999999999999999999" })
    void malformedJsonPointersDoNotSelectAValue(String pointer) {
        try (var handler = handler("sm",
                new Provider(resource -> "{\"a~2\":\"wrong\",\"a~\":\"wrong\",\"~~1\":\"wrong\",\"a\":[\"zero\",\"one\"]}"))) {
            var config = config(handler, Map.of("value", "${sm::secret#" + pointer + "}"));
            assertThrows(IllegalStateException.class, () -> config.getValue("value", String.class));
        }
    }

    @Test
    void objectKeysAreNotRestrictedToArrayIndexSyntaxAndEscapesAreDecodedOnce() {
        try (var handler = handler("sm",
                new Provider(
                        resource -> "{\"a\":{\"01\":\"object-key\"},\"~1\":\"tilde-escape\",\"~~1\":\"double-tilde\",\"\":false}"))) {
            var config = config(handler, Map.of("object", "${sm::secret#/a/01}", "tilde", "${sm::secret#/~01}",
                    "double", "${sm::secret#/~0~01}", "empty", "${sm::secret#/}"));
            assertEquals("object-key", config.getValue("object", String.class));
            assertEquals("tilde-escape", config.getValue("tilde", String.class));
            assertEquals("double-tilde", config.getValue("double", String.class));
            assertEquals("false", config.getValue("empty", String.class));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "sm", "ssm" })
    void smallRyePreservesArnSelectorsRawContentAndJsonPointers(String name) {
        var calls = new ArrayList<String>();
        String arn = "arn:aws:ssm:us-east-1:123456789012:parameter/db:3";
        var provider = new Provider(resource -> {
            calls.add(resource);
            return resource.equals(arn) ? " raw\n${literal}=value " : "{\"a/b\":{\"~key\":[\"password\",true,123]}}";
        });
        try (var handler = handler(name, provider)) {
            var config = config(handler, Map.of("raw", "${" + name + "::" + arn + "}",
                    "selected", "${" + name + "::prod/db#/a~1b/~0key/0}",
                    "boolean", "${" + name + "::prod/db#/a~1b/~0key/1}",
                    "number", "${" + name + "::prod/db#/a~1b/~0key/2}"));
            assertEquals(0, provider.calls);
            assertEquals(" raw\n${literal}=value ", config.getValue("raw", String.class));
            assertEquals("password", config.getValue("selected", String.class));
            assertEquals("true", config.getValue("boolean", String.class));
            assertEquals("123", config.getValue("number", String.class));
            assertEquals(List.of(arn, "prod/db"), calls);
            assertTrue(handler.getPropertyNames().isEmpty());
            assertTrue(handler.getProperties().isEmpty());
            assertNull(handler.getValue(arn));
            assertFalse(config.getPropertyNames().iterator().next().equals(arn));
        }
        assertEquals(1, provider.closes);
    }

    @ParameterizedTest
    @ValueSource(strings = { "PT0S", "PT0.000000001S" })
    void zeroOrExpiredTtlFetchesAgainAndNeverServesStaleOnFailure(String ttl) {
        var provider = new Provider(resource -> "fresh");
        try (var handler = handler("sm", provider)) {
            var config = config(handler, Map.of("value", "${sm::secret}", "quarkus.secretsmanager.expression.cache-ttl", ttl));
            assertEquals("fresh", config.getValue("value", String.class));
            provider.fetch = resource -> {
                throw new IllegalStateException("unsafe secret");
            };
            var failure = assertThrows(IllegalStateException.class, () -> config.getValue("value", String.class));
            assertNull(failure.getCause());
            assertFalse(failure.getMessage().contains("unsafe"));
            provider.fetch = resource -> "rotated";
            assertEquals("rotated", config.getValue("value", String.class));
            assertEquals(3, provider.calls);
        }
    }

    @Test
    void concurrentReadsShareOneLazyClientAndOneFetchAndCloseIsFinal() throws Exception {
        var starts = new AtomicInteger();
        var provider = new Provider(resource -> "value");
        var handler = new AwsConfigExpression("sm", "secretsmanager", context -> {
            starts.incrementAndGet();
            return provider;
        });
        var config = config(handler, Map.of("value", "${sm::secret}"));
        assertEquals(0, starts.get());
        var executor = Executors.newFixedThreadPool(8);
        try {
            var results = new ArrayList<java.util.concurrent.Future<String>>();
            for (int i = 0; i < 64; i++) {
                results.add(executor.submit(() -> config.getValue("value", String.class)));
            }
            for (var result : results) {
                assertEquals("value", result.get(5, TimeUnit.SECONDS));
            }
            assertEquals(1, starts.get());
            assertEquals(1, provider.calls);
            handler.close();
            handler.close();
            assertEquals(1, provider.closes);
            assertThrows(IllegalStateException.class, () -> config.getValue("value", String.class));
            assertEquals(1, provider.closes);
        } finally {
            handler.close();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void cacheIsBoundedAndUnusedHandlerNeverOpensClient() {
        var provider = new Provider(Function.identity());
        try (var handler = handler("sm", provider)) {
            var config = config(handler, Map.of("value", "${sm::first}"));
            assertEquals("first", config.getValue("value", String.class));
            var decoder = handler.getSecretKeysHandler(null);
            for (int i = 0; i < 256; i++) {
                assertEquals("secret" + i, decoder.decode("secret" + i));
            }
            assertEquals("first", config.getValue("value", String.class));
            assertEquals(258, provider.calls);
        }
        try (var handler = new AwsConfigExpression("sm", "secretsmanager", context -> {
            throw new AssertionError("Unused handler initialized a client");
        })) {
            config(handler, Map.of("local", "value"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "raw", "{broken secret", "{\"key\":null}", "{\"key\":[]}", "{\"other\":\"unsafe\"}",
            "{\"key\":\"unsafe\"} trailing", "{\"key\":\"\\ud800\"}" })
    void jsonSelectionErrorsNeverIncludeRemoteContentOrParserCauses(String content) {
        try (var handler = handler("sm", new Provider(resource -> content))) {
            var config = config(handler, Map.of("value", "${sm::secret#/key}"));
            var failure = assertThrows(IllegalStateException.class, () -> config.getValue("value", String.class));
            assertEquals("Unable to resolve AWS sm expression; check resource, permissions, content and client settings.",
                    failure.getMessage());
            assertNull(failure.getCause());
            assertEquals(0, failure.getSuppressed().length);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "${sm::secret#invalid}", "${sm::#/key}" })
    void invalidSelectionSyntaxFailsBeforeClientInitialization(String expression) {
        try (var handler = new AwsConfigExpression("sm", "secretsmanager", context -> {
            throw new AssertionError("Invalid selector opened a client");
        })) {
            var config = config(handler, Map.of("value", expression));
            assertThrows(IllegalStateException.class, () -> config.getValue("value", String.class));
        }
    }

    @Test
    void invalidCacheDurationFailsBeforeClientInitialization() {
        try (var handler = new AwsConfigExpression("sm", "secretsmanager", context -> {
            throw new AssertionError("Invalid duration opened a client");
        })) {
            var config = config(handler, Map.of("value", "${sm::secret}",
                    "quarkus.secretsmanager.expression.cache-ttl", "-1S"));
            assertThrows(IllegalStateException.class, () -> config.getValue("value", String.class));
        }
    }

    private static AwsConfigExpression handler(String name, Provider provider) {
        return new AwsConfigExpression(name, name.equals("sm") ? "secretsmanager" : "ssm", context -> provider);
    }

    @Test
    void rawTextDoesNotBypassStrictUnicodeValidation() {
        var provider = new Provider(resource -> Character.toString((char) 0xD800));
        try (var handler = handler("sm", provider)) {
            var config = config(handler, Map.of("value", "${sm::secret}"));
            var failure = assertThrows(IllegalStateException.class, () -> config.getValue("value", String.class));
            assertNull(failure.getCause());
            assertEquals(1, provider.closes);
        }
    }

    @Test
    void failedJsonSelectionIsNotCachedAndCleanupCannotLeakSdkMessages() {
        var provider = new Provider(resource -> "invalid remote secret");
        try (var handler = handler("sm", provider)) {
            var config = config(handler, Map.of("value", "${sm::secret#/key}"));
            assertThrows(IllegalStateException.class, () -> config.getValue("value", String.class));
            assertEquals(1, provider.closes);
            provider.fetch = resource -> "{\"key\":\"recovered\"}";
            assertEquals("recovered", config.getValue("value", String.class));
            assertEquals(2, provider.calls);
        }
        var handler = new AwsConfigExpression("sm", "secretsmanager", context -> new AwsConfigExpression.ValueProvider() {
            public String fetch(String resource) {
                throw new IllegalStateException("unsafe fetch message");
            }

            public void close() {
                throw new IllegalStateException("unsafe close message");
            }
        });
        var config = config(handler, Map.of("value", "${sm::secret}"));
        var failure = assertThrows(IllegalStateException.class, () -> config.getValue("value", String.class));
        assertEquals("Unable to resolve AWS sm expression; check resource, permissions, content and client settings.",
                failure.getMessage());
        assertNull(failure.getCause());
        assertEquals(0, failure.getSuppressed().length);
        handler.close();
    }

    private static SmallRyeConfig config(AwsConfigExpression handler, Map<String, String> values) {
        return new SmallRyeConfigBuilder().addDefaultInterceptors().withSecretKeysHandlers(new LazySecretKeysHandler(handler))
                .withSources(handler).withSources(new MapBackedConfigSource("test", values) {
                }).build();
    }

    private static class Provider implements AwsConfigExpression.ValueProvider {
        private Function<String, String> fetch;
        private int calls;
        private int closes;

        Provider(Function<String, String> fetch) {
            this.fetch = fetch;
        }

        public String fetch(String resource) {
            calls++;
            return fetch.apply(resource);
        }

        public void close() {
            closes++;
        }
    }
}
