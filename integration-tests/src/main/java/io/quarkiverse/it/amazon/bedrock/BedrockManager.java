package io.quarkiverse.it.amazon.bedrock;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import software.amazon.awssdk.services.bedrock.BedrockAsyncClient;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrock.model.FoundationModelSummary;
import software.amazon.awssdk.services.bedrock.model.ListFoundationModelsResponse;

@Singleton
public class BedrockManager {
    @Inject
    BedrockClient sync;

    @Inject
    BedrockAsyncClient async;

    public List<FoundationModelSummary> listFoundationModels() {
        final ListFoundationModelsResponse response = sync.listFoundationModels(ignore -> {
        });
        return response.modelSummaries();
    }

    public CompletableFuture<List<FoundationModelSummary>> listFoundationModelsAsync() {
        return async.listFoundationModels(ignore -> {
        })
                .thenApply(ListFoundationModelsResponse::modelSummaries);
    }

}
