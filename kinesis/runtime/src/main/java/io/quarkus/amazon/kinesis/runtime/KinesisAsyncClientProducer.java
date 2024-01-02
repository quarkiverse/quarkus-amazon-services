package io.quarkus.amazon.kinesis.runtime;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;

import io.quarkus.arc.DefaultBean;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClientBuilder;

@ApplicationScoped
public class KinesisAsyncClientProducer {
    private final KinesisAsyncClient asyncClient;

    KinesisAsyncClientProducer(Instance<KinesisAsyncClientBuilder> asyncClientBuilderInstance) {
        this.asyncClient = asyncClientBuilderInstance.isResolvable() ? asyncClientBuilderInstance.get().build() : null;
    }

    @DefaultBean
    @Produces
    @ApplicationScoped
    public KinesisAsyncClient asyncClient() {
        if (asyncClient == null) {
            throw new IllegalStateException("The KinesisAsyncClient is required but has not been detected/configured.");
        }
        return asyncClient;
    }

    @PreDestroy
    public void destroy() {
        if (asyncClient != null) {
            asyncClient.close();
        }
    }
}
