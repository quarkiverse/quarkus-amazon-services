package io.quarkus.it.amazon.sts;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.jboss.logging.Logger;

import software.amazon.awssdk.services.sts.StsAsyncClient;
import software.amazon.awssdk.services.sts.StsClient;

@Path("/sts")
public class StsResource {

    private static final Logger LOG = Logger.getLogger(StsResource.class);
    public final static String TEXT = "Quarkus is awsome";

    @Inject
    StsClient stsClient;

    @Inject
    StsAsyncClient stsAsyncClient;

    @GET
    @Path("sync")
    @Produces(TEXT_PLAIN)
    public String testSync() {
        LOG.info("TODO: Testing Sync STS client");

        return TEXT;
    }

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsync() {
        LOG.info("TODO: Testing Async STS client");

        return CompletableFuture.supplyAsync(() -> TEXT);
    }
}
