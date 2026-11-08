package io.quarkiverse.amazon.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.amazon.common.runtime.AwsConfigExpression;
import io.smallrye.config.SmallRyeConfigBuilder;

class AwsConfigSourceCloserTest {
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void closesEverySourceExactlyOnceEvenIfOneProviderFails(boolean failFirst) {
        var first = new AtomicInteger();
        var second = new AtomicInteger();
        var expressionCloses = new AtomicInteger();
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        ClassLoader isolated = new ClassLoader(previous) {
        };
        var resolver = ConfigProviderResolver.instance();
        try (var a = new Source("first", 300, first, failFirst); var b = new Source("second", 200, second, false)) {
            var expression = new AwsConfigExpression("sm", "secretsmanager",
                    context -> new AwsConfigExpression.ValueProvider() {
                        public String fetch(String resource) {
                            return "value";
                        }

                        public void close() {
                            expressionCloses.incrementAndGet();
                        }
                    });
            var config = new SmallRyeConfigBuilder().addDefaultInterceptors().withSources(a, b, expression)
                    .withSecretKeyHandlerFactories(expression).withDefaultValue("value", "${sm::secret}").build();
            assertEquals("value", config.getValue("value", String.class));
            resolver.registerConfig(config, isolated);
            Thread.currentThread().setContextClassLoader(isolated);
            try {
                var closer = new AwsConfigSourceCloser();
                closer.onStop(null);
                assertEquals(1, first.get());
                assertEquals(1, second.get());
                assertEquals(1, expressionCloses.get());
                closer.onStop(null);
                assertEquals(1, first.get());
                assertEquals(1, second.get());
                assertEquals(1, expressionCloses.get());
            } finally {
                Thread.currentThread().setContextClassLoader(previous);
                resolver.releaseConfig(config);
            }
        }
    }

    private static final class Source extends AbstractAwsConfigSource {
        Source(String name, int ordinal, AtomicInteger closes, boolean fail) {
            super(name, ordinal, new AwsConfigProvider() {
                public FetchResult fetchAll() {
                    return new FetchResult(Map.of(), Set.of(), Set.of(), Map.of(), null, Duration.ZERO);
                }

                public void close() {
                    closes.incrementAndGet();
                    if (fail) {
                        throw new IllegalStateException("unsafe SDK message");
                    }
                }
            }, id -> List.of(), Duration.ofHours(1), "test-close-" + name);
        }
    }
}
