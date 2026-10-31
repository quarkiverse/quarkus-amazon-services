package io.quarkiverse.amazon.common.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.ThreadingModel;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;

class VertxContextAwareExecutorTest {
    private Vertx vertx;
    private ExecutorService sdk;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(1));
        sdk = Executors.newSingleThreadExecutor(r -> new Thread(r, "sdk-completion"));
    }

    @AfterEach
    void close() throws Exception {
        sdk.shutdownNow();
        vertx.close().await(10, TimeUnit.SECONDS);
    }

    record Observed(Context context, Thread thread, String value, Throwable failure) {
    }

    private CompletableFuture<Observed> call(boolean enabled, Supplier<String> response) {
        ExecutionAttributes attributes = new ExecutionAttributes();
        new VertxContextCapturingInterceptor(enabled).beforeExecution(null, attributes);
        CompletableFuture<String> source = new CompletableFuture<>();
        CompletableFuture<Observed> observed = new CompletableFuture<>();
        AsyncCompletionContext.complete(source, attributes).whenComplete((value, failure) -> observed
                .complete(new Observed(Vertx.currentContext(), Thread.currentThread(), value, failure)));
        sdk.execute(() -> {
            try {
                source.complete(response.get());
            } catch (RuntimeException failure) {
                source.completeExceptionally(failure);
            }
        });
        return observed;
    }

    private Observed onContext(Context context, boolean enabled, Supplier<String> response) throws Exception {
        CompletableFuture<Observed> observed = new CompletableFuture<>();
        context.runOnContext(ignored -> call(enabled, response).whenComplete((result, failure) -> {
            if (failure == null)
                observed.complete(result);
            else
                observed.completeExceptionally(failure);
        }));
        return observed.get(10, TimeUnit.SECONDS);
    }

    @Test
    void returnsSuccessAndFailureToTheCallerEventLoop() throws Exception {
        Context caller = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        assertSame(caller, onContext(caller, true, () -> "ok").context());
        Observed failed = onContext(caller, true, () -> {
            throw new IllegalStateException("boom");
        });
        assertSame(caller, failed.context());
        assertInstanceOf(IllegalStateException.class, failed.failure());
    }

    @Test
    void nonVertxCallsKeepTheSdkExecutor() throws Exception {
        Observed result = call(true, () -> "ok").get(10, TimeUnit.SECONDS);
        assertNull(result.context());
        assertEquals("sdk-completion", result.thread().getName());
    }

    @Test
    void executeBlockingCanWaitWithASingleWorker() throws Exception {
        Context caller = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        Observed result = caller.executeBlocking(() -> call(true, () -> "ok")
                .get(5, TimeUnit.SECONDS)).await(10, TimeUnit.SECONDS);
        assertNull(result.context());
        assertEquals("ok", result.value());
    }

    @Test
    void workerVerticleCanWaitWithoutSerializingItsOwnCompletion() throws Exception {
        CompletableFuture<Observed> observed = new CompletableFuture<>();
        vertx.deployVerticle(new AbstractVerticle() {
            @Override
            public void start() throws Exception {
                assertTrue(context.isWorkerContext());
                Observed result = call(true, () -> "ok").get(5, TimeUnit.SECONDS);
                observed.complete(result);
            }
        }, new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER)).await(10, TimeUnit.SECONDS);
        Observed result = observed.get(10, TimeUnit.SECONDS);
        assertEquals("ok", result.value());
        assertNull(result.context());
    }

    @Test
    void doesNotChangeTheSafetyOfAnExistingCallerContext() throws Exception {
        Context caller = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        VertxContextSafetyToggle.setContextSafe(caller, false);
        assertSame(caller, onContext(caller, true, () -> "ok").context());
        assertTrue(VertxContextSafetyToggle.isExplicitlyMarkedAsUnsafe(caller));
    }

    @Test
    void concurrentRequestsKeepTheirOwnContexts() throws Exception {
        List<CompletableFuture<Void>> checks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Context caller = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
            CompletableFuture<Void> check = new CompletableFuture<>();
            checks.add(check);
            caller.runOnContext(
                    ignored -> call(true, () -> "ok").whenComplete((result, failure) -> {
                        if (failure != null)
                            check.completeExceptionally(failure);
                        else if (result.context() != caller)
                            check.completeExceptionally(new AssertionError("Wrong caller context"));
                        else
                            check.complete(null);
                    }));
        }
        CompletableFuture.allOf(checks.toArray(CompletableFuture[]::new)).get(10, TimeUnit.SECONDS);
    }

    @Test
    void disabledDoesNotCaptureADispatcher() {
        ExecutionAttributes attributes = new ExecutionAttributes();
        new VertxContextCapturingInterceptor(false).beforeExecution(null, attributes);
        assertNull(attributes.getAttribute(AsyncCompletionContext.EXECUTOR));
    }
}
