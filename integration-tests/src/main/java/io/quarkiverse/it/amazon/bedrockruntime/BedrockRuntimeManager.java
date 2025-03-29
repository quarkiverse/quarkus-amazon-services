package io.quarkiverse.it.amazon.bedrockruntime;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.AsyncInvokeSummary;
import software.amazon.awssdk.services.bedrockruntime.model.ListAsyncInvokesResponse;

@Singleton
public class BedrockRuntimeManager {
    @Inject
    BedrockRuntimeClient sync;

    @Inject
    BedrockRuntimeAsyncClient async;

    public List<AsyncInvokeSummary> listAsyncInvokes() {
        final ListAsyncInvokesResponse response = sync.listAsyncInvokes(i -> i.maxResults(1));
        return response.asyncInvokeSummaries();
    }

    public CompletableFuture<List<AsyncInvokeSummary>> listAsyncInvokesAsync() {
        return async.listAsyncInvokes(i -> i.maxResults(1))
                .thenApply(ListAsyncInvokesResponse::asyncInvokeSummaries);
    }

}
