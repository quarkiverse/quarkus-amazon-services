package io.quarkiverse.amazon.fake.deployment;

import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkiverse.amazon.common.test.AbstractLifecycleResource;
import software.amazon.awssdk.services.fake.FakeAsyncClient;
import software.amazon.awssdk.services.fake.FakeClient;

@Path("/lifecycle")
public class LifecycleResource extends AbstractLifecycleResource {
    @Inject
    FakeClient sync;

    @Inject
    @AmazonClient("archive")
    FakeAsyncClient async;

    @Override
    protected FakeClient sync() {
        return sync;
    }

    @Override
    protected FakeAsyncClient async() {
        return async;
    }

    @Override
    protected CompletionStage<?> request() {
        return async.listThings(request -> {
        });
    }
}
