package io.quarkiverse.it.amazon.bedrock;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/bedrock")
@Produces(MediaType.APPLICATION_JSON)
public class BedrockResource {
    @Inject
    BedrockManager bedrockManager;

    @Path("sync/guardrails/")
    @GET
    public Map<String, Object> guardrailLifecycle(@QueryParam("name") String name) {
        return bedrockManager.guardrailLifecycle(name);
    }

    @Path("async/guardrails/")
    @GET
    public CompletableFuture<Map<String, Object>> guardrailLifecycleAsync(@QueryParam("name") String name) {
        return bedrockManager.guardrailLifecycleAsync(name);
    }
}
