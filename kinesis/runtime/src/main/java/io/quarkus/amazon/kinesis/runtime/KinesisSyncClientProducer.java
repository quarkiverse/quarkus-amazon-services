package io.quarkus.amazon.kinesis.runtime;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;

import io.quarkus.arc.DefaultBean;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.KinesisClientBuilder;

@ApplicationScoped
public class KinesisSyncClientProducer {
    private final KinesisClient syncClient;

    KinesisSyncClientProducer(Instance<KinesisClientBuilder> syncClientBuilderInstance) {
        this.syncClient = syncClientBuilderInstance.isResolvable() ? syncClientBuilderInstance.get().build() : null;
    }

    @DefaultBean
    @Produces
    @ApplicationScoped
    public KinesisClient client() {
        if (syncClient == null) {
            throw new IllegalStateException("The KinesisClient is required but has not been detected/configured.");
        }
        return syncClient;
    }

    @PreDestroy
    public void destroy() {
        if (syncClient != null) {
            syncClient.close();
        }
    }
}
