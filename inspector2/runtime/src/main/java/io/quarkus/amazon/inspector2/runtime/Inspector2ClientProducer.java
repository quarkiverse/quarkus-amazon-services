package io.quarkus.amazon.inspector2.runtime;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;

import io.quarkus.arc.DefaultBean;
import software.amazon.awssdk.services.inspector2.Inspector2AsyncClient;
import software.amazon.awssdk.services.inspector2.Inspector2AsyncClientBuilder;
import software.amazon.awssdk.services.inspector2.Inspector2Client;
import software.amazon.awssdk.services.inspector2.Inspector2ClientBuilder;

@ApplicationScoped
public class Inspector2ClientProducer {
    private final Inspector2Client syncClient;
    private final Inspector2AsyncClient asyncClient;

    Inspector2ClientProducer(Instance<Inspector2ClientBuilder> syncClientBuilderInstance,
            Instance<Inspector2AsyncClientBuilder> asyncClientBuilderInstance) {
        this.syncClient = syncClientBuilderInstance.isResolvable() ? syncClientBuilderInstance.get().build() : null;
        this.asyncClient = asyncClientBuilderInstance.isResolvable() ? asyncClientBuilderInstance.get().build() : null;
    }

    @DefaultBean
    @Produces
    @ApplicationScoped
    public Inspector2Client client() {
        if (syncClient == null) {
            throw new IllegalStateException("The Inspector2Client is required but has not been detected/configured.");
        }
        return syncClient;
    }

    @DefaultBean
    @Produces
    @ApplicationScoped
    public Inspector2AsyncClient asyncClient() {
        if (asyncClient == null) {
            throw new IllegalStateException("The Inspector2AsyncClient is required but has not been detected/configured.");
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
