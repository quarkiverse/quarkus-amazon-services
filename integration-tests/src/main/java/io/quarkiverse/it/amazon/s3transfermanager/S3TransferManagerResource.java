package io.quarkiverse.it.amazon.s3transfermanager;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.logging.Logger;

import io.quarkiverse.amazon.common.runtime.AsyncHttpClientBuildTimeConfig.AsyncClientType;
import io.quarkiverse.amazon.s3.runtime.S3BuildTimeConfig;
import io.quarkiverse.amazon.s3.runtime.S3Crt;
import io.quarkiverse.it.amazon.s3.S3Utils;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

@Path("/s3-transfer-manager")
public class S3TransferManagerResource {
    private static final String ASYNC_BUCKET_SOURCE = "async-" + UUID.randomUUID().toString();
    private static final String ASYNC_BUCKET_DESTINATION = "async-" + UUID.randomUUID().toString();
    private static final String CRT_ASYNC_BUCKET_SOURCE = "crt-async-" + UUID.randomUUID().toString();
    private static final String CRT_ASYNC_BUCKET_DESTINATION = "crt-async-" + UUID.randomUUID().toString();
    private static final String SAMPLE_S3_OBJECT = "sample S3 object";

    private static final Logger LOG = Logger.getLogger(S3TransferManagerResource.class);

    @Inject
    S3BuildTimeConfig s3Config;

    @Inject
    S3AsyncClient s3AsyncClient;

    @Inject
    @S3Crt
    Instance<S3AsyncClient> s3CrtAsyncClientInstance;

    @Inject
    S3TransferManager s3TransferManagerAsyncClient;

    @Inject
    @S3Crt
    Instance<S3TransferManager> s3CrtTransferManagerAsyncClientInstance;

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsyncS3() {
        LOG.info("Testing Async S3 Transfer Manager with bucket: " + ASYNC_BUCKET_SOURCE);
        String keyValue = UUID.randomUUID().toString();

        return S3Utils.createBucketAsync(s3AsyncClient, ASYNC_BUCKET_SOURCE)
                .thenCompose(bucket -> S3Utils.createBucketAsync(s3AsyncClient, ASYNC_BUCKET_DESTINATION))
                .thenCompose(bucket -> s3AsyncClient.putObject(S3Utils.createPutRequest(ASYNC_BUCKET_SOURCE, keyValue),
                        AsyncRequestBody.fromString(SAMPLE_S3_OBJECT)))
                .thenCompose(bucket -> s3TransferManagerAsyncClient
                        .copy(S3TransferManagerUtils.createCopyObjectRequest(ASYNC_BUCKET_SOURCE,
                                keyValue, ASYNC_BUCKET_DESTINATION, keyValue))
                        .completionFuture())
                .thenCompose(resp -> s3AsyncClient.getObject(S3Utils.createGetRequest(ASYNC_BUCKET_DESTINATION, keyValue),
                        AsyncResponseTransformer.toBytes()))
                .thenApply(resp -> resp.asUtf8String())
                .exceptionally(th -> {
                    LOG.error("Error during async S3 operations", th.getCause());
                    return "ERROR";
                });
    }

    @GET
    @Path("crt-async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testCrtAsyncS3() {
        LOG.info("Testing Crt Async S3 Transfer Manager client with bucket: " + CRT_ASYNC_BUCKET_SOURCE);
        String keyValue = UUID.randomUUID().toString();

        try {
            S3AsyncClient s3CrtAsyncClient = s3CrtAsyncClientInstance.get();
            S3TransferManager s3CrtTransferManagerAsyncClient = s3CrtTransferManagerAsyncClientInstance.get();
            return S3Utils.createBucketAsync(s3CrtAsyncClient, CRT_ASYNC_BUCKET_SOURCE)
                    .thenCompose(bucket -> S3Utils.createBucketAsync(s3AsyncClient, CRT_ASYNC_BUCKET_DESTINATION))
                    .thenCompose(
                            bucket -> s3CrtAsyncClient.putObject(S3Utils.createPutRequest(CRT_ASYNC_BUCKET_SOURCE, keyValue),
                                    AsyncRequestBody.fromString(SAMPLE_S3_OBJECT)))
                    .thenCompose(bucket -> s3CrtTransferManagerAsyncClient.copy(S3TransferManagerUtils
                            .createCopyObjectRequest(CRT_ASYNC_BUCKET_SOURCE, keyValue, CRT_ASYNC_BUCKET_DESTINATION, keyValue))
                            .completionFuture())
                    .thenCompose(
                            resp -> s3AsyncClient.getObject(S3Utils.createGetRequest(CRT_ASYNC_BUCKET_DESTINATION, keyValue),
                                    AsyncResponseTransformer.toBytes()))
                    .thenApply(resp -> resp.asUtf8String())
                    .exceptionally(th -> {
                        LOG.error("Error during async S3 operations", th.getCause());
                        return "ERROR";
                    });
        } catch (UnsatisfiedResolutionException ex) {
            if (s3Config.asyncClient().type().equals(AsyncClientType.AWS_CRT)) {
                LOG.error("Error during async S3 operations", ex);
                return CompletableFuture.completedStage("ERROR");
            } else
                return CompletableFuture.completedStage(ex.getMessage());
        }
    }
}
