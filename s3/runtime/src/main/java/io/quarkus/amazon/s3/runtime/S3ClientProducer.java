package io.quarkus.amazon.s3.runtime;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;

import io.quarkus.arc.DefaultBean;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@ApplicationScoped
public class S3ClientProducer {
    private final S3Client syncClient;
    private final S3AsyncClient asyncClient;
    private final S3Presigner presigner;

    S3ClientProducer(Instance<S3ClientBuilder> syncClientBuilderInstance,
            Instance<S3AsyncClientBuilder> asyncClientBuilderInstance,
            Instance<S3Presigner.Builder> presignerBuilder) {
        this.syncClient = syncClientBuilderInstance.isResolvable() ? syncClientBuilderInstance.get().build() : null;
        this.asyncClient = asyncClientBuilderInstance.isResolvable() ? asyncClientBuilderInstance.get().build() : null;
        this.presigner = presignerBuilder.isResolvable() ? presignerBuilder.get().build() : null;
    }

    @DefaultBean
    @Produces
    @ApplicationScoped
    public S3Client client() {
        if (syncClient == null) {
            throw new IllegalStateException("The S3Client is required but has not been detected/configured.");
        }
        return syncClient;
    }

    @DefaultBean
    @Produces
    @ApplicationScoped
    public S3AsyncClient asyncClient() {
        if (asyncClient == null) {
            throw new IllegalStateException("The S3AsyncClient is required but has not been detected/configured.");
        }
        return asyncClient;
    }

    @DefaultBean
    @Produces
    @ApplicationScoped
    public S3Presigner presigner() {
        if (presigner == null) {
            throw new IllegalStateException("The S3Presigner is required but has not been detected/configured.");
        }
        return presigner;
    }

    @PreDestroy
    public void destroy() {
        if (syncClient != null) {
            syncClient.close();
        }
        if (asyncClient != null) {
            asyncClient.close();
        }
        if (presigner != null) {
            presigner.close();
        }
    }
}
