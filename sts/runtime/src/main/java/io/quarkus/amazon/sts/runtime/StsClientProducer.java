package io.quarkus.amazon.sts.runtime;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;

import software.amazon.awssdk.services.sts.StsAsyncClient;
import software.amazon.awssdk.services.sts.StsAsyncClientBuilder;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.StsClientBuilder;

@ApplicationScoped
public class StsClientProducer {
    private final StsClient syncClient;
    private final StsAsyncClient asyncClient;

    public StsClientProducer(Instance<StsClientBuilder> syncClientBuilderInstance,
            Instance<StsAsyncClientBuilder> asyncClientBuilderInstance) {
        this.syncClient = syncClientBuilderInstance.isResolvable() ? syncClientBuilderInstance.get()
                .build() : null;
        this.asyncClient = asyncClientBuilderInstance.isResolvable() ? asyncClientBuilderInstance.get()
                .build() : null;
    }

    @Produces
    @ApplicationScoped
    public StsClient client() {
        if (syncClient == null) {
            throw new IllegalStateException(
                    "The StsClient is required but has not been detected/configured.");
        }
        return syncClient;
    }

    @Produces
    @ApplicationScoped
    public StsAsyncClient asyncClient() {
        if (asyncClient == null) {
            throw new IllegalStateException(
                    "The StsAsyncClient is required but has not been detected/configured.");
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
