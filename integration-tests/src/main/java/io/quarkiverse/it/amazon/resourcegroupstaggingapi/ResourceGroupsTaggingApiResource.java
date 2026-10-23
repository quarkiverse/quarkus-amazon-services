package io.quarkiverse.it.amazon.resourcegroupstaggingapi;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.logging.Logger;

import software.amazon.awssdk.services.resourcegroupstaggingapi.ResourceGroupsTaggingApiAsyncClient;
import software.amazon.awssdk.services.resourcegroupstaggingapi.ResourceGroupsTaggingApiClient;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.GetResourcesResponse;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.ResourceTagMapping;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

@Path("/resourcegroupstaggingapi")
public class ResourceGroupsTaggingApiResource {

    private static final Logger LOG = Logger.getLogger(ResourceGroupsTaggingApiResource.class);

    private static final String SYNC_QUEUE_NAME = "quarkus-rgta-sync-queue";
    private static final String ASYNC_QUEUE_NAME = "quarkus-rgta-async-queue";
    private static final String TAG_KEY = "quarkus-rgta";

    @Inject
    ResourceGroupsTaggingApiClient resourceGroupsTaggingApiClient;

    @Inject
    ResourceGroupsTaggingApiAsyncClient resourceGroupsTaggingApiAsyncClient;

    @Inject
    SqsClient sqsClient;

    @Inject
    SqsAsyncClient sqsAsyncClient;

    @GET
    @Path("sync")
    @Produces(TEXT_PLAIN)
    public String testSync() {
        LOG.info("Testing Sync Resource Groups Tagging API client");
        // Create the resource to tag
        String queueUrl = sqsClient.createQueue(q -> q.queueName(SYNC_QUEUE_NAME)).queueUrl();
        String queueArn = sqsClient
                .getQueueAttributes(a -> a.queueUrl(queueUrl).attributeNames(QueueAttributeName.QUEUE_ARN))
                .attributes().get(QueueAttributeName.QUEUE_ARN);

        // Tag the resource
        var failedResourcesMap = resourceGroupsTaggingApiClient
                .tagResources(r -> r.resourceARNList(queueArn).tags(Map.of(TAG_KEY, "sync")))
                .failedResourcesMap();
        if (!failedResourcesMap.isEmpty()) {
            throw new IllegalStateException("Failed to tag resources: " + failedResourcesMap);
        }

        // Find the resource by tag
        List<String> taggedArns = arns(resourceGroupsTaggingApiClient
                .getResources(r -> r.tagFilters(f -> f.key(TAG_KEY).values("sync"))));
        List<String> tagValues = resourceGroupsTaggingApiClient.getTagValues(r -> r.key(TAG_KEY)).tagValues();
        if (!tagValues.contains("sync")) {
            throw new IllegalStateException("Tag value not found: " + tagValues);
        }

        // Untag the resource
        resourceGroupsTaggingApiClient.untagResources(r -> r.resourceARNList(queueArn).tagKeys(TAG_KEY));
        List<String> remainingArns = arns(resourceGroupsTaggingApiClient
                .getResources(r -> r.tagFilters(f -> f.key(TAG_KEY).values("sync"))));
        if (!remainingArns.isEmpty()) {
            throw new IllegalStateException("Resources still tagged: " + remainingArns);
        }

        sqsClient.deleteQueue(q -> q.queueUrl(queueUrl));
        return String.join(",", taggedArns);
    }

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsync() {
        LOG.info("Testing Async Resource Groups Tagging API client");
        // Create the resource to tag
        return sqsAsyncClient.createQueue(q -> q.queueName(ASYNC_QUEUE_NAME))
                .thenCompose(queue -> sqsAsyncClient
                        .getQueueAttributes(a -> a.queueUrl(queue.queueUrl()).attributeNames(QueueAttributeName.QUEUE_ARN))
                        .thenApply(attributes -> attributes.attributes().get(QueueAttributeName.QUEUE_ARN))
                        .thenCompose(this::tagFindAndUntagAsync)
                        .thenCompose(result -> sqsAsyncClient.deleteQueue(q -> q.queueUrl(queue.queueUrl()))
                                .thenApply(deleted -> result)));
    }

    private CompletionStage<String> tagFindAndUntagAsync(String queueArn) {
        // Tag the resource
        return resourceGroupsTaggingApiAsyncClient
                .tagResources(r -> r.resourceARNList(queueArn).tags(Map.of(TAG_KEY, "async")))
                .thenCompose(tagged -> tagged.failedResourcesMap().isEmpty()
                        ? CompletableFuture.completedFuture(tagged)
                        : CompletableFuture.failedFuture(
                                new IllegalStateException("Failed to tag resources: " + tagged.failedResourcesMap())))
                // Find the resource by tag
                .thenCompose(tagged -> resourceGroupsTaggingApiAsyncClient
                        .getResources(r -> r.tagFilters(f -> f.key(TAG_KEY).values("async"))))
                .thenApply(ResourceGroupsTaggingApiResource::arns)
                // Untag the resource
                .thenCompose(taggedArns -> resourceGroupsTaggingApiAsyncClient
                        .untagResources(r -> r.resourceARNList(queueArn).tagKeys(TAG_KEY))
                        .thenCompose(untagged -> resourceGroupsTaggingApiAsyncClient
                                .getResources(r -> r.tagFilters(f -> f.key(TAG_KEY).values("async"))))
                        .thenCompose(remaining -> remaining.resourceTagMappingList().isEmpty()
                                ? CompletableFuture.completedFuture(String.join(",", taggedArns))
                                : CompletableFuture.failedFuture(
                                        new IllegalStateException("Resources still tagged: " + arns(remaining)))));
    }

    private static List<String> arns(GetResourcesResponse response) {
        return response.resourceTagMappingList().stream()
                .map(ResourceTagMapping::resourceARN)
                .collect(Collectors.toList());
    }
}
