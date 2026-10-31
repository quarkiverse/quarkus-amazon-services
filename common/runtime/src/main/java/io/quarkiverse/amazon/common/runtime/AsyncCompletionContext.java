package io.quarkiverse.amazon.common.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.jboss.logging.Logger;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.Abortable;

/**
 * Connects the capturing interceptor to the SDK's transformed final-result boundary.
 * No Vert.x types are referenced here, so unconfigured clients and applications without Vert.x keep their executor.
 */
public final class AsyncCompletionContext {

    static final ExecutionAttribute<Executor> EXECUTOR = new ExecutionAttribute<>("QuarkusCompletionExecutor");
    private static final Logger LOG = Logger.getLogger(AsyncCompletionContext.class);

    private AsyncCompletionContext() {
    }

    public static <T> CompletableFuture<T> complete(CompletableFuture<T> source, ExecutionAttributes attributes) {
        return complete(source, attributes.getAttribute(EXECUTOR));
    }

    /** Must be called on the caller's thread, which is the thread of the captured context. */
    static <T> CompletableFuture<T> complete(CompletableFuture<T> source, Executor executor) {
        if (executor == null || source.isDone()) {
            // Already completed, e.g. a validation error: the caller's continuations run on its own context
            return source;
        }
        CompletableFuture<T> result = new CompletableFuture<>();
        source.whenComplete((value, failure) -> {
            try {
                executor.execute(() -> deliver(result, value, failure));
            } catch (RuntimeException dispatchFailure) {
                // Includes a closed Vert.x instance: the result must not be lost.
                LOG.debug("Could not dispatch async result to Vert.x; completing on the SDK thread", dispatchFailure);
                deliver(result, value, failure);
            }
        });
        result.whenComplete((value, failure) -> {
            if (failure != null) {
                // Preserve SDK cancellation/abort propagation, including callers using CompletableFuture.orTimeout().
                source.completeExceptionally(failure);
            }
        });
        return result;
    }

    private static <T> void deliver(CompletableFuture<T> result, T value, Throwable failure) {
        boolean accepted = failure == null ? result.complete(value) : result.completeExceptionally(failure);
        if (!accepted && failure == null) {
            // The caller cancelled or timed out before the delivery: nobody will release a streaming response
            release(value);
        }
    }

    private static void release(Object value) {
        try {
            if (value instanceof Abortable abortable) {
                // e.g. a ResponseInputStream: do not drain the connection
                abortable.abort();
            } else if (value instanceof AutoCloseable closeable) {
                closeable.close();
            } else if (value instanceof SdkPublisher<?> publisher) {
                // e.g. a ResponsePublisher: cancelling its subscription releases the connection
                publisher.subscribe(new Subscriber<Object>() {
                    @Override
                    public void onSubscribe(Subscription subscription) {
                        subscription.cancel();
                    }

                    @Override
                    public void onNext(Object item) {
                    }

                    @Override
                    public void onError(Throwable failure) {
                    }

                    @Override
                    public void onComplete() {
                    }
                });
            }
        } catch (Exception releaseFailure) {
            LOG.debug("Failed to release an async result that was no longer awaited", releaseFailure);
        }
    }
}
