package io.quarkiverse.amazon.s3.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.quarkiverse.amazon.s3.runtime.S3Config;
import io.quarkiverse.amazon.s3.runtime.S3CrtRecorder;
import io.quarkus.runtime.RuntimeValue;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;
import software.amazon.awssdk.services.s3.S3AsyncClient;

class S3CrtCompletionPoolTest {
    @Test
    void enabledUsesTheConfiguredPool() throws Exception {
        checkPool(true);
    }

    @Test
    void disabledRunsAttemptCompletionWithoutThePool() throws Exception {
        checkPool(false);
    }

    private void checkPool(boolean enabled) throws Exception {
        S3Config config = new SmallRyeConfigBuilder().addDefaultInterceptors().withMapping(S3Config.class)
                .withSources(new MapBackedConfigSource("test", Map.of(
                        "quarkus.s3.async-client.advanced.use-future-completion-thread-pool", Boolean.toString(enabled))) {
                }).build().getConfigMapping(S3Config.class);
        AtomicInteger pooled = new AtomicInteger();
        AtomicInteger completed = new AtomicInteger();
        var builder = S3AsyncClient.crtBuilder();
        new S3CrtRecorder(new RuntimeValue<>(config)).setExecutor(new RuntimeValue<>(builder), command -> {
            pooled.incrementAndGet();
            command.run();
        });
        // The SDK's CRT builder does not expose a completion-executor getter.
        var field = builder.getClass().getDeclaredField("futureCompletionExecutor");
        field.setAccessible(true);
        ((Executor) field.get(builder)).execute(completed::incrementAndGet);
        assertEquals(1, completed.get());
        assertEquals(enabled ? 1 : 0, pooled.get());
    }
}
