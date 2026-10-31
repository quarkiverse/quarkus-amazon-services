package io.quarkiverse.amazon.s3.runtime;

import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;

/**
 * Build-time bridge to the CRT builder's existing, package-private interceptor registration method.
 */
public interface S3CrtContextAwareBuilder {

    void addContextInterceptor(ExecutionInterceptor interceptor);
}
