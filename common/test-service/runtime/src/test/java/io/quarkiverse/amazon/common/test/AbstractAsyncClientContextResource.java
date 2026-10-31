package io.quarkiverse.amazon.common.test;

import java.util.concurrent.CompletableFuture;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

public abstract class AbstractAsyncClientContextResource {
    protected abstract CompletableFuture<?> call();

    @GET
    @Path("blocking")
    @Blocking
    public String blocking() {
        Context caller = Vertx.currentContext();
        boolean worker = Context.isOnWorkerThread();
        return call().handle((response, failure) -> worker + "|" + (Vertx.currentContext() == caller)
                + "|" + Context.isOnEventLoopThread()).join();
    }

    @GET
    @Path("virtual")
    @RunOnVirtualThread
    public String virtual() {
        Context caller = Vertx.currentContext();
        boolean virtual = Thread.currentThread().isVirtual();
        return call().handle((response, failure) -> virtual + "|" + (Vertx.currentContext() == caller)
                + "|" + Context.isOnEventLoopThread()).join();
    }
}
