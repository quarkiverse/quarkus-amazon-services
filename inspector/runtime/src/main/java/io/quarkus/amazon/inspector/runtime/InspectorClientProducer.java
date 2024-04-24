package io.quarkus.amazon.inspector.runtime;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;

import io.quarkus.arc.DefaultBean;
import software.amazon.awssdk.services.inspector.InspectorAsyncClient;
import software.amazon.awssdk.services.inspector.InspectorAsyncClientBuilder;
import software.amazon.awssdk.services.inspector.InspectorClient;
import software.amazon.awssdk.services.inspector.InspectorClientBuilder;

@ApplicationScoped
public class InspectorClientProducer {
    private final InspectorClient syncClient;
    private final InspectorAsyncClient asyncClient;

    InspectorClientProducer(Instance<InspectorClientBuilder> syncClientBuilderInstance,
            Instance<InspectorAsyncClientBuilder> asyncClientBuilderInstance) {
        this.syncClient = syncClientBuilderInstance.isResolvable() ? syncClientBuilderInstance.get().build() : null;
        this.asyncClient = asyncClientBuilderInstance.isResolvable() ? asyncClientBuilderInstance.get().build() : null;
    }

    @DefaultBean
    @Produces
    @ApplicationScoped
    public InspectorClient client() {
        if (syncClient == null) {
            throw new IllegalStateException("The InspectorClient is required but has not been detected/configured.");
        }
        return syncClient;
    }

    @DefaultBean
    @Produces
    @ApplicationScoped
    public InspectorAsyncClient asyncClient() {
        if (asyncClient == null) {
            throw new IllegalStateException("The InspectorAsyncClient is required but has not been detected/configured.");
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
