package io.quarkiverse.it.amazon.bedrockruntime;

import java.util.concurrent.CompletableFuture;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/bedrockruntime")
public class BedrockRuntimeResource {
    @Inject
    BedrockRuntimeManager bedrockRuntimeManager;

    @Path("sync/converse/")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String converse(@QueryParam("prompt") String prompt) {
        return bedrockRuntimeManager.converse(prompt);
    }

    @Path("async/converse/")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public CompletableFuture<String> converseAsync(@QueryParam("prompt") String prompt) {
        return bedrockRuntimeManager.converseAsync(prompt);
    }

    @Path("sync/invoke/")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String invokeModel(@QueryParam("prompt") String prompt) {
        return bedrockRuntimeManager.invokeModel(prompt);
    }

    @Path("async/invoke/")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public CompletableFuture<String> invokeModelAsync(@QueryParam("prompt") String prompt) {
        return bedrockRuntimeManager.invokeModelAsync(prompt);
    }

    @Path("async/converse-stream/")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public CompletableFuture<String> converseStreamAsync(@QueryParam("prompt") String prompt) {
        return bedrockRuntimeManager.converseStreamAsync(prompt);
    }
}
