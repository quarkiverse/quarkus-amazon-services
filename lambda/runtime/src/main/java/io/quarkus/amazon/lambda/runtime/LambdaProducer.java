package io.quarkus.amazon.lambda.runtime;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;

import io.quarkus.arc.DefaultBean;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.LambdaAsyncClientBuilder;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.LambdaClientBuilder;

@ApplicationScoped
public class LambdaProducer {
    private final LambdaClient syncClient;
    private final LambdaAsyncClient asyncClient;

    LambdaProducer(
            final Instance<LambdaClientBuilder> syncClientBuilderInstance,
            final Instance<LambdaAsyncClientBuilder> asyncClientBuilderInstance) {
        syncClient = syncClientBuilderInstance.isResolvable() ? syncClientBuilderInstance.get().build() : null;
        asyncClient = asyncClientBuilderInstance.isResolvable() ? asyncClientBuilderInstance.get().build() : null;
    }

    @DefaultBean
    @Produces
    @ApplicationScoped
    public LambdaClient client() {
        if (syncClient == null) {
            throw new IllegalStateException(
                    "The LambdaClient is required but has not been detected/configured.");
        }
        return syncClient;
    }

    @DefaultBean
    @Produces
    @ApplicationScoped
    public LambdaAsyncClient asyncClient() {
        if (asyncClient == null) {
            throw new IllegalStateException(
                    "The LambdaAsyncClient is required but has not been detected/configured.");
        }
        return asyncClient;
    }

    @PreDestroy
    public void destroy() {
        if (syncClient != null) {
            syncClient.close();
        }
        if (asyncClient != null) {
            asyncClient.close();
        }
    }
}
