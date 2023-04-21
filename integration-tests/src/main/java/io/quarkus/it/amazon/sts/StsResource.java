package io.quarkus.it.amazon.sts;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.CompletionStage;

import org.jboss.logging.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import software.amazon.awssdk.services.sts.StsAsyncClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;

@Path("/sts")
public class StsResource {

    private static final Logger LOG = Logger.getLogger(StsResource.class);

    @Inject
    StsClient stsClient;

    @Inject
    StsAsyncClient stsAsyncClient;

    @GET
    @Path("sync")
    @Produces(TEXT_PLAIN)
    public String testSync() {
        LOG.info("Testing Sync STS client");
        AssumeRoleResponse assumeRoleResponse = stsClient
                .assumeRole(builder -> builder.roleArn("arn:aws:sts::000000000000:assumed-role/test-role")
                        .roleSessionName("session-test").build());
        return assumeRoleResponse.assumedRoleUser().arn();
    }

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsync() {
        LOG.info("Testing Async STS client");
        return stsAsyncClient.assumeRole(builder -> builder.roleArn("arn:aws:sts::000000000000:assumed-role/test-role")
                .roleSessionName("session-test").build())
                .thenApply(response -> response.assumedRoleUser().arn());
    }
}
