package io.quarkiverse.amazon.sdk.deployment;

import java.util.concurrent.CompletableFuture;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

import io.quarkiverse.amazon.common.test.AbstractAsyncClientContextResource;
import software.amazon.awssdk.services.ecr.EcrAsyncClient;

@Path("/async-context")
public class AsyncClientContextResource extends AbstractAsyncClientContextResource {
    @Inject
    EcrAsyncClient client;

    @Override
    protected CompletableFuture<?> call() {
        return client.describeRepositories();
    }
}
