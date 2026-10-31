package io.quarkiverse.amazon.s3.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.sun.net.httpserver.HttpServer;

import io.quarkiverse.amazon.common.runtime.VertxContextCapturingInterceptor;
import io.quarkiverse.amazon.s3.runtime.S3Crt;
import io.quarkiverse.amazon.s3.runtime.S3CrtContextAwareBuilder;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;

public class S3CrtClientVertxContextTest {

    @Inject
    @S3Crt
    S3AsyncClient client;

    @Inject
    Vertx vertx;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(CaptureProbeInterceptor.class)
                    .addAsResource("crt-vertx-context-config.properties", "application.properties"));

    @Test
    public void completesSuccessfulResponseOnCallerContext() throws Exception {
        checkSuccessfulResponse(false);
    }

    @Test
    public void streamingResponseCompletesOnCallerContext() throws Exception {
        checkSuccessfulResponse(true);
    }

    private void checkSuccessfulResponse(boolean streaming) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] body = (streaming ? "streamed body"
                    : "<ListAllMyBucketsResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"
                            + "<Buckets/></ListAllMyBucketsResult>")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Length", Integer.toString(body.length));
            exchange.getResponseHeaders().set("ETag", "\"test-etag\"");
            exchange.getResponseHeaders().set("Last-Modified", "Wed, 21 Oct 2015 07:28:00 GMT");
            if (exchange.getRequestMethod().equals("HEAD")) {
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
                return;
            }
            boolean ranged = streaming && exchange.getRequestHeaders().getFirst("Range") != null;
            if (ranged) {
                exchange.getResponseHeaders().set("Content-Range", "bytes 0-" + (body.length - 1) + "/" + body.length);
            }
            exchange.sendResponseHeaders(ranged ? 206 : 200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            S3CrtAsyncClientBuilder builder = S3AsyncClient.crtBuilder()
                    .endpointOverride(URI.create("http://localhost:" + server.getAddress().getPort()))
                    .region(Region.US_EAST_1)
                    .credentialsProvider(
                            StaticCredentialsProvider.create(AwsBasicCredentials.create("test-key", "test-secret")))
                    .futureCompletionExecutor(ForkJoinPool.commonPool());
            ((S3CrtContextAwareBuilder) builder)
                    .addContextInterceptor(new VertxContextCapturingInterceptor(true));
            try (S3AsyncClient local = builder.build()) {
                Context duplicated = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
                CompletableFuture<Context> completion = new CompletableFuture<>();
                CompletableFuture<Object> value = new CompletableFuture<>();
                duplicated.runOnContext(ignored -> {
                    CompletableFuture<?> request = streaming
                            ? local.getObject(b -> b.bucket("bucket").key("key"), AsyncResponseTransformer.toBytes())
                            : local.listBuckets();
                    request.whenComplete((response, failure) -> {
                        if (failure != null) {
                            completion.completeExceptionally(failure);
                        } else {
                            completion.complete(Vertx.currentContext());
                            value.complete(response);
                        }
                    });
                });
                assertSame(duplicated, completion.get(30, TimeUnit.SECONDS));
                if (streaming)
                    assertEquals("streamed body", ((ResponseBytes<?>) value.get(5, TimeUnit.SECONDS)).asUtf8String());
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void copyObjectDispatchesOnlyItsFinalResult() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().set("ETag", "\"test-etag\"");
            exchange.getResponseHeaders().set("Last-Modified", "Wed, 21 Oct 2015 07:28:00 GMT");
            if (exchange.getRequestMethod().equals("HEAD")) {
                // The CRT client's copy starts with a HEAD request to choose between a single and a multipart copy
                exchange.getResponseHeaders().set("Content-Length", "10");
                exchange.sendResponseHeaders(200, -1);
            } else {
                byte[] body = "<CopyObjectResult><ETag>\"copied\"</ETag><LastModified>2026-01-01T00:00:00Z</LastModified></CopyObjectResult>"
                        .getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.start();
        try {
            S3CrtAsyncClientBuilder builder = S3AsyncClient.crtBuilder()
                    .endpointOverride(URI.create("http://localhost:" + server.getAddress().getPort()))
                    .region(Region.US_EAST_1)
                    .forcePathStyle(true)
                    .credentialsProvider(
                            StaticCredentialsProvider.create(AwsBasicCredentials.create("test-key", "test-secret")))
                    .futureCompletionExecutor(ForkJoinPool.commonPool());
            CaptureProbeInterceptor probe = new CaptureProbeInterceptor();
            ((S3CrtContextAwareBuilder) builder)
                    .addContextInterceptor(new VertxContextCapturingInterceptor(true));
            ((S3CrtContextAwareBuilder) builder).addContextInterceptor(probe);
            try (S3AsyncClient local = builder.build()) {
                Context duplicated = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
                CompletableFuture<Context> completion = new CompletableFuture<>();
                duplicated.runOnContext(ignored -> local.copyObject(r -> r.sourceBucket("bucket").sourceKey("key")
                        .destinationBucket("bucket").destinationKey("copy")).whenComplete((response, failure) -> {
                            if (failure != null) {
                                completion.completeExceptionally(failure);
                            } else {
                                completion.complete(Vertx.currentContext());
                            }
                        }));
                assertSame(duplicated, completion.get(30, TimeUnit.SECONDS));
                // Only the composite result is dispatched, not the requests the SDK makes on its own behalf
                assertEquals(Boolean.FALSE, probe.captured.get("HeadObjectRequest"));
                assertEquals(Boolean.FALSE, probe.captured.get("CopyObjectRequest"));
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void completesOnCallerDuplicatedContext() throws Exception {
        Context duplicated = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        CompletableFuture<Thread> callerThread = new CompletableFuture<>();
        CompletableFuture<Thread> completionThread = new CompletableFuture<>();
        CompletableFuture<Context> completionContext = new CompletableFuture<>();
        CompletableFuture<Throwable> failure = new CompletableFuture<>();

        // nothing listens on the endpoint, the call fails on a CRT thread
        duplicated.runOnContext(ignored -> {
            callerThread.complete(Thread.currentThread());
            client.listBuckets().whenComplete((r, t) -> {
                completionThread.complete(Thread.currentThread());
                completionContext.complete(Vertx.currentContext());
                failure.complete(t);
            });
        });

        assertNotNull(failure.get(60, TimeUnit.SECONDS));
        assertSame(duplicated, completionContext.get());
        assertSame(callerThread.get(), completionThread.get());
    }

    @Test
    public void completesOutsideOfVertxContext() throws Exception {
        CompletableFuture<Context> completionContext = new CompletableFuture<>();
        CompletableFuture<Throwable> failure = new CompletableFuture<>();

        client.listBuckets().whenComplete((r, t) -> {
            completionContext.complete(Vertx.currentContext());
            failure.complete(t);
        });

        assertNotNull(failure.get(60, TimeUnit.SECONDS));
        assertNull(completionContext.get());
    }
}
