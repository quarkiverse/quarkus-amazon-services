package io.quarkiverse.it.amazon.resourcegroupstaggingapi;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.logging.Logger;

import software.amazon.awssdk.services.resourcegroupstaggingapi.ResourceGroupsTaggingApiAsyncClient;
import software.amazon.awssdk.services.resourcegroupstaggingapi.ResourceGroupsTaggingApiClient;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.TagResourcesResponse;

@Path("/resourcegroupstaggingapi")
public class ResourceGroupsTaggingApiResource {

    private static final Logger LOG = Logger.getLogger(ResourceGroupsTaggingApiResource.class);

    @Inject
    ResourceGroupsTaggingApiClient resourceGroupsTaggingApiClient;

    @Inject
    ResourceGroupsTaggingApiAsyncClient resourceGroupsTaggingApiAsyncClient;

    @GET
    @Path("sync")
    @Produces(TEXT_PLAIN)
    public String testSync() {
        LOG.info("Testing Sync Resource Groups Tagging API client");
        // Create Resource Group
        var failedResourcesMap = resourceGroupsTaggingApiClient
                .tagResources(r -> r
                        .resourceARNList("arn:aws:resource-group:us-east-1:123456789012:resource-group:TestResourceGroup"))
                .failedResourcesMap();
        // Get Resource Group
        return failedResourcesMap.get("arn:aws:resource-group:us-east-1:123456789012:resource-group:TestResourceGroup")
                .errorCodeAsString();
    }

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsync() {
        LOG.info("Testing Async Resource Groups Tagging API client");
        // Create Resource Group
        return resourceGroupsTaggingApiAsyncClient
                .tagResources(r -> r
                        .resourceARNList("arn:aws:resource-group:us-east-1:123456789012:resource-group:TestResourceGroup"))
                .thenApply(TagResourcesResponse::failedResourcesMap)
                .thenApply(failedResourcesMap -> failedResourcesMap
                        .get("arn:aws:resource-group:us-east-1:123456789012:resource-group:TestResourceGroup").errorMessage());
    }
}
