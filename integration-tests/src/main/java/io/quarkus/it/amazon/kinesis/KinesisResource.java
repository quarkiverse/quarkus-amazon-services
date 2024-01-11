package io.quarkus.it.amazon.kinesis;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.logging.Logger;

import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisClient;

@Path("/kinesis")
public class KinesisResource {

    private static final Logger LOG = Logger.getLogger(KinesisResource.class);
    public final static String STREAM_NAME = "quarkus-stream-";

    @Inject
    KinesisClient kinesisClient;

    @Inject
    KinesisAsyncClient kinesisAsyncClient;

    @GET
    @Path("sync")
    @Produces(TEXT_PLAIN)
    public String testSync() {
        LOG.info("Testing Sync Kinesis client");

        try {
            kinesisClient.createStream(builder -> builder.streamName(STREAM_NAME + "sync").shardCount(1));
            return kinesisClient.describeStream(builder -> builder.streamName(STREAM_NAME + "sync")).streamDescription()
                    .streamARN();
        } catch (UnsupportedOperationException ex) {
            return ex.getMessage();
        }
    }

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsync() {
        LOG.info("Testing Async Kinesis client");

        try {
            return kinesisAsyncClient.createStream(builder -> builder.streamName(STREAM_NAME + "async").shardCount(1))
                    .thenCompose(
                            discard -> kinesisAsyncClient.describeStream(builder -> builder.streamName(STREAM_NAME + "async"))
                                    .thenApply(r -> r.streamDescription().streamARN()));
        } catch (UnsupportedOperationException ex) {
            return CompletableFuture.completedStage(ex.getMessage());
        }
    }
}
