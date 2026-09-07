package io.quarkiverse.it.amazon.ecs;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.logging.Logger;

import software.amazon.awssdk.services.ecs.EcsAsyncClient;
import software.amazon.awssdk.services.ecs.EcsClient;

@Path("/ecs")
public class EcsResource {

    private static final Logger LOG = Logger.getLogger(EcsResource.class);

    private static final String CLUSTER_NAME = "quarkus-test-cluster";

    @Inject
    EcsClient ecsClient;

    @Inject
    EcsAsyncClient ecsAsyncClient;

    @GET
    @Path("sync")
    @Produces(TEXT_PLAIN)
    public String testSync() {
        LOG.info("Testing Sync ECS client");
        // Create cluster
        ecsClient.createCluster(r -> r.clusterName(CLUSTER_NAME));
        // List clusters and verify our cluster exists
        var clusterArns = ecsClient.listClusters().clusterArns();
        return clusterArns.stream()
                .filter(arn -> arn.contains(CLUSTER_NAME))
                .findFirst()
                .orElse("NOT FOUND");
    }

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsync() {
        LOG.info("Testing Async ECS client");
        // Create cluster then list clusters and verify
        return ecsAsyncClient
                .createCluster(r -> r.clusterName(CLUSTER_NAME))
                .thenCompose(response -> ecsAsyncClient.listClusters())
                .thenApply(response -> response.clusterArns().stream()
                        .filter(arn -> arn.contains(CLUSTER_NAME))
                        .findFirst()
                        .orElse("NOT FOUND"));
    }
}