package io.quarkiverse.it.amazon.bedrock;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import software.amazon.awssdk.services.bedrock.model.BedrockException;
import software.amazon.awssdk.services.bedrock.model.FoundationModelSummary;

@Path("/bedrock")
public class BedrockResource {
    @Inject
    BedrockManager bedrockRuntimeManager;

    @Path("sync/foundation-models/")
    @GET
    public Response listAsyncInvoked() {
        try {
            final List<FoundationModelSummary> list = bedrockRuntimeManager.listFoundationModels();
            final String result = list.stream().map(FoundationModelSummary::modelId).collect(Collectors.joining(","));
            return Response.ok(result).build();
        } catch (BedrockException e) {
            if (e.getMessage().contains("501")) {
                // pro feature
                return Response.status(501).build();
            }
        }
        return Response.status(500).build();
    }

    @Path("async/foundation-models/")
    @GET
    public CompletableFuture<Response> listAsyncInvokedAsync() {
        return bedrockRuntimeManager.listFoundationModelsAsync()
                .thenApply(list -> Response.ok(list.stream()
                        .map(FoundationModelSummary::modelId)
                        .collect(Collectors.joining(","))).build())
                .exceptionally(e -> {
                    if (e.getMessage().contains("501")) {
                        // pro feature
                        return Response.status(501).build();
                    }
                    return Response.status(500).build();
                });
    }
}
