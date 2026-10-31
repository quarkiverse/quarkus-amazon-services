package io.quarkiverse.amazon.common.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;

class AsyncHttpClientConfigTest {
    private static final String PROPAGATION = "test.vertx-context-propagation";
    private static final String POOL = "test.use-future-completion-thread-pool";

    @Test
    void contextPropagationIsEnabledByDefault() {
        assertTrue(advanced(Map.of()).vertxContextPropagation());
    }

    @Test
    void contextPropagationAcceptsBooleanValues() {
        for (boolean enabled : new boolean[] { true, false }) {
            assertEquals(enabled, advanced(Map.of(PROPAGATION, Boolean.toString(enabled))).vertxContextPropagation());
        }
    }

    @Test
    void contextPropagationAndCompletionPoolRemainIndependent() {
        var propagatedWithoutPool = advanced(Map.of(PROPAGATION, "true", POOL, "false"));
        assertTrue(propagatedWithoutPool.vertxContextPropagation());
        assertFalse(propagatedWithoutPool.useFutureCompletionThreadPool());
        var pooledWithoutPropagation = advanced(Map.of(PROPAGATION, "false", POOL, "true"));
        assertFalse(pooledWithoutPropagation.vertxContextPropagation());
        assertTrue(pooledWithoutPropagation.useFutureCompletionThreadPool());
    }

    private AsyncHttpClientConfig.Advanced advanced(Map<String, String> properties) {
        return new SmallRyeConfigBuilder().withMapping(AsyncHttpClientConfig.Advanced.class, "test")
                .withSources(new MapBackedConfigSource("test", properties) {
                }).build().getConfigMapping(AsyncHttpClientConfig.Advanced.class, "test");
    }
}
