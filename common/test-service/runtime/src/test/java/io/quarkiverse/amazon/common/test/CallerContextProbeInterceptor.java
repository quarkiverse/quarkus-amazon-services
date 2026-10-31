package io.quarkiverse.amazon.common.test;

import java.util.concurrent.atomic.AtomicReference;

import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;

/**
 * Records whether the Vert.x caller context was captured, i.e. whether the interceptor capturing it ran for the execution.
 */
public class CallerContextProbeInterceptor implements ExecutionInterceptor {

    /**
     * The name of the execution attribute containing the final-result dispatcher.
     */
    private static final String CALLER_CONTEXT_ATTRIBUTE = "QuarkusCompletionExecutor";

    public static final AtomicReference<Boolean> CAPTURED = new AtomicReference<>();

    @Override
    public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        CAPTURED.set(executionAttributes.getAttributes().entrySet().stream()
                .anyMatch(entry -> CALLER_CONTEXT_ATTRIBUTE.equals(entry.getKey().toString()) && entry.getValue() != null));
    }
}
