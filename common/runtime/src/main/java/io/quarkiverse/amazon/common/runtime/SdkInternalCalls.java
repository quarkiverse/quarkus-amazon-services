package io.quarkiverse.amazon.common.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import software.amazon.awssdk.core.SdkClient;

/**
 * Tracks the calls a composite operation of the SDK, e.g. a multipart upload, makes synchronously on behalf of the
 * application.
 * <p>
 * Their results are consumed by SDK code, which may perform blocking or CPU-intensive work (body splitting, checksums), so
 * they are not dispatched to a Vert.x event loop: only the final result of the composite operation is. The calls the SDK
 * chains later run from its completion threads, which are not event loops, so they are not dispatched either.
 */
public final class SdkInternalCalls {

    private static final ThreadLocal<Scope> SCOPE = new ThreadLocal<>();

    /** @param composite the executor capturing the caller's context for the final result of the composite operation */
    private record Scope(Executor composite, Scope previous) {
    }

    private SdkInternalCalls() {
    }

    static boolean active() {
        return SCOPE.get() != null;
    }

    /**
     * Called by the transformed composite operations of an SDK client on entry.
     * <p>
     * The caller's context is captured for the composite result, and the calls the operation makes synchronously are
     * internal: the transformed method calls {@link #exitComposite} on return, or {@link #abortComposite} if it throws.
     */
    public static void enterComposite(SdkClient client) {
        VertxContextCapturingInterceptor interceptor = VertxContextCapturingInterceptor.find(client);
        Executor executor = interceptor == null ? null : interceptor.capture();
        SCOPE.set(new Scope(executor, SCOPE.get()));
    }

    public static <T> CompletableFuture<T> exitComposite(CompletableFuture<T> source) {
        Scope scope = SCOPE.get();
        // Leave the scope last: if wrapping fails, the transformed method's handler leaves it
        CompletableFuture<T> result = AsyncCompletionContext.complete(source, scope.composite());
        leave(scope);
        return result;
    }

    public static void abortComposite() {
        leave(SCOPE.get());
    }

    private static void leave(Scope scope) {
        if (scope.previous() == null) {
            SCOPE.remove();
        } else {
            SCOPE.set(scope.previous());
        }
    }
}
