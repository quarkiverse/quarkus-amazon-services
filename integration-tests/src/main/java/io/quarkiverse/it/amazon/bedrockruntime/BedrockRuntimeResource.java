package io.quarkiverse.it.amazon.bedrockruntime;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import software.amazon.awssdk.services.bedrockruntime.model.AsyncInvokeSummary;
import software.amazon.awssdk.services.bedrockruntime.model.BedrockRuntimeException;

@Path("/bedrockruntime")
public class BedrockRuntimeResource {
    @Inject
    BedrockRuntimeManager bedrockRuntimeManager;

    @Path("sync/list-invokes/")
    @GET
    public Response listAsyncInvoked() {
        try {
            final List<AsyncInvokeSummary> list = bedrockRuntimeManager.listAsyncInvokes();
            final String result = list.stream().map(AsyncInvokeSummary::modelArn).collect(Collectors.joining(","));
            return Response.ok(result).build();
        } catch (BedrockRuntimeException e) {
            if (e.getMessage().contains("501")) {
                // pro feature
                return Response.status(501).build();
            }
        }
        return Response.status(500).build();
    }

    @Path("async/list-invokes/")
    @GET
    public CompletableFuture<Response> listAsyncInvokedAsync() {
        return bedrockRuntimeManager.listAsyncInvokesAsync()
                .thenApply(list -> Response.ok(list.stream()
                        .map(AsyncInvokeSummary::modelArn)
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
