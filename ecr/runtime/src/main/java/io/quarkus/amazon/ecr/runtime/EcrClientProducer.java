package io.quarkus.amazon.ecr.runtime;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;

import io.quarkus.arc.DefaultBean;
import software.amazon.awssdk.services.ecr.EcrAsyncClient;
import software.amazon.awssdk.services.ecr.EcrAsyncClientBuilder;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.EcrClientBuilder;

@ApplicationScoped
public class EcrClientProducer {
    private final EcrClient syncClient;
    private final EcrAsyncClient asyncClient;

    EcrClientProducer(Instance<EcrClientBuilder> syncClientBuilderInstance,
            Instance<EcrAsyncClientBuilder> asyncClientBuilderInstance) {
        this.syncClient = syncClientBuilderInstance.isResolvable() ? syncClientBuilderInstance.get().build() : null;
        this.asyncClient = asyncClientBuilderInstance.isResolvable() ? asyncClientBuilderInstance.get().build() : null;
    }

    @DefaultBean
    @Produces
    @ApplicationScoped
    public EcrClient client() {
        if (syncClient == null) {
            throw new IllegalStateException("The EcrClient is required but has not been detected/configured.");
        }
        return syncClient;
    }

    @DefaultBean
    @Produces
    @ApplicationScoped
    public EcrAsyncClient asyncClient() {
        if (asyncClient == null) {
            throw new IllegalStateException("The EcrAsyncClient is required but has not been detected/configured.");
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
