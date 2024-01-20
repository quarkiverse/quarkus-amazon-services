package io.quarkus.amazon.s3.runtime;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;

import io.quarkus.arc.DefaultBean;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

@ApplicationScoped
public class S3TransferManagerProducer {

    private final S3TransferManager transferManagerWithAsyncClient;

    S3TransferManagerProducer(Instance<S3AsyncClient> asyncClientInstance) {
        this.transferManagerWithAsyncClient = asyncClientInstance.isResolvable() ? S3TransferManager.builder()
                .s3Client(asyncClientInstance.get()).build() : null;
    }

    @DefaultBean
    @Produces
    @ApplicationScoped
    public S3TransferManager client() {
        if (transferManagerWithAsyncClient == null) {
            throw new IllegalStateException("The S3AsyncClient is required but has not been detected/configured.");
        }
        return transferManagerWithAsyncClient;
    }

    @PreDestroy
    public void destroy() {
        if (transferManagerWithAsyncClient != null) {
            transferManagerWithAsyncClient.close();
        }
    }
}
