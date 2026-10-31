package io.quarkiverse.amazon.s3.deployment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;

/**
 * Records, per request type, whether the result of a request was captured to be dispatched to the caller's Vert.x context.
 */
public class CaptureProbeInterceptor implements ExecutionInterceptor {

    /** The name of the execution attribute containing the final-result dispatcher. */
    private static final String CALLER_CONTEXT_ATTRIBUTE = "QuarkusCompletionExecutor";

    final Map<String, Boolean> captured = new ConcurrentHashMap<>();

    @Override
    public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
        boolean dispatched = executionAttributes.getAttributes().entrySet().stream()
                .anyMatch(entry -> CALLER_CONTEXT_ATTRIBUTE.equals(entry.getKey().toString()) && entry.getValue() != null);
        captured.merge(context.request().getClass().getSimpleName(), dispatched, Boolean::logicalOr);
    }
}
