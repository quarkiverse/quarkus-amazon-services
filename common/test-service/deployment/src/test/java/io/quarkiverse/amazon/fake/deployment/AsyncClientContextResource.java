package io.quarkiverse.amazon.fake.deployment;

import java.util.concurrent.CompletableFuture;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

import io.quarkiverse.amazon.common.test.AbstractAsyncClientContextResource;
import software.amazon.awssdk.services.fake.FakeAsyncClient;

@Path("/async-context")
public class AsyncClientContextResource extends AbstractAsyncClientContextResource {
    @Inject
    FakeAsyncClient client;

    @Override
    protected CompletableFuture<?> call() {
        return client.listThings();
    }
}
