package io.quarkiverse.amazon.common.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

public abstract class AbstractAsyncClientVertxContextDisabledTest {

    @Test
    public void blockingEndpointKeepsTheSdkCompletionBehavior() {
        given().get("/async-context/blocking").then().statusCode(200).body(is("true|false|false"));
    }

    @Test
    public void virtualThreadEndpointKeepsTheSdkCompletionBehavior() {
        given().get("/async-context/virtual").then().statusCode(200).body(is("true|false|false"));
    }

    protected abstract CompletableFuture<?> call();

    @Inject
    Vertx vertx;

    @Test
    public void doesNotCompleteOnCallerContext() throws Exception {
        CallerContextProbeInterceptor.CAPTURED.set(null);
        Context duplicated = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        CompletableFuture<Context> completionContext = new CompletableFuture<>();
        CompletableFuture<Throwable> failure = new CompletableFuture<>();

        // nothing listens on the endpoint, the call fails on an SDK thread
        duplicated.runOnContext(ignored -> call().whenComplete((r, t) -> {
            completionContext.complete(Vertx.currentContext());
            failure.complete(t);
        }));

        assertNotNull(failure.get(30, TimeUnit.SECONDS));
        assertNull(completionContext.get());
        // the interceptor capturing the context is not registered
        assertFalse(CallerContextProbeInterceptor.CAPTURED.get());
    }
}
