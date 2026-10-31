package io.quarkiverse.amazon.common.runtime;

import java.util.concurrent.Executor;

import org.jboss.logging.Logger;

import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;

/**
 * Captures the dispatcher used to complete the final result of an async call.
 * <p>
 * {@link #beforeExecution} runs on the caller's thread. This interceptor is registered on the async clients built by the
 * extension only when the Vert.x extension is present and Vert.x
 * context propagation is enabled.
 */
public final class VertxContextCapturingInterceptor implements ExecutionInterceptor {
    private static final Logger LOG = Logger.getLogger(VertxContextCapturingInterceptor.class);

    private final boolean enabled;

    public VertxContextCapturingInterceptor(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
        executionAttributes.putAttribute(AsyncCompletionContext.EXECUTOR, capture());
    }

    /**
     * @return the executor completing the result returned to the application on the caller's context, or {@code null} to
     *         keep the SDK completion, e.g. for the calls the SDK makes on its own behalf
     */
    Executor capture() {
        if (SdkInternalCalls.active()) {
            return null;
        }
        try {
            return VertxContextAwareExecutor.capture(enabled);
        } catch (RuntimeException failure) {
            // Never fail the AWS request because of context propagation.
            LOG.debug("Could not capture the Vert.x context; completing on the SDK thread", failure);
            return null;
        }
    }

    /**
     * Looks up the interceptor of a client, for the transformed composite operations of the client.
     *
     * @return the interceptor, or {@code null} if the client does not propagate the Vert.x context
     */
    public static VertxContextCapturingInterceptor find(SdkClient client) {
        try {
            for (ExecutionInterceptor interceptor : client.serviceClientConfiguration().overrideConfiguration()
                    .executionInterceptors()) {
                if (interceptor instanceof VertxContextCapturingInterceptor capturing) {
                    return capturing;
                }
            }
        } catch (RuntimeException failure) {
            LOG.debug("Could not resolve the Vert.x context propagation of " + client.getClass(), failure);
        }
        return null;
    }
}
