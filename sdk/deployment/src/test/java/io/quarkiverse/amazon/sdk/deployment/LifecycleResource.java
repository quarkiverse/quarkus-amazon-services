package io.quarkiverse.amazon.sdk.deployment;

import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkiverse.amazon.common.test.AbstractLifecycleResource;
import software.amazon.awssdk.services.ecr.EcrAsyncClient;
import software.amazon.awssdk.services.ecr.EcrClient;

@Path("/lifecycle")
public class LifecycleResource extends AbstractLifecycleResource {
    @Inject
    EcrClient sync;

    @Inject
    @AmazonClient("archive")
    EcrAsyncClient async;

    @Override
    protected EcrClient sync() {
        return sync;
    }

    @Override
    protected EcrAsyncClient async() {
        return async;
    }

    @Override
    protected CompletionStage<?> request() {
        return async.describeRepositories(request -> {
        });
    }
}
