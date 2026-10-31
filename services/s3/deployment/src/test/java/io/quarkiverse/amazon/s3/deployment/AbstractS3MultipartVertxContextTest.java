package io.quarkiverse.amazon.s3.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;

import com.sun.net.httpserver.HttpServer;

import io.quarkiverse.amazon.common.runtime.SdkInternalCalls;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;

public abstract class AbstractS3MultipartVertxContextTest {
    @Inject
    S3AsyncClient discoveredClient; // Discover the extension's client and its provided builder without building it twice.
    @Inject
    MultipartClient multipart;
    @Inject
    Vertx vertx;

    static QuarkusExtensionTest configure(boolean enabled) {
        return new QuarkusExtensionTest()
                .withApplicationRoot(jar -> jar.addClasses(MultipartClient.class, CaptureProbeInterceptor.class))
                .overrideConfigKey("quarkus.devservices.enabled", "false")
                .overrideConfigKey("quarkus.s3.aws.region", "us-east-1")
                .overrideConfigKey("quarkus.s3.aws.credentials.type", "anonymous")
                .overrideConfigKey("quarkus.s3.path-style-access", "true")
                .overrideConfigKey("quarkus.s3.async-client.type", "netty")
                .overrideConfigKey("quarkus.s3.async-client.advanced.vertx-context-propagation", Boolean.toString(enabled));
    }

    protected boolean enabled() {
        return true;
    }

    record Observed(Context context, String thread, Throwable failure) {
    }

    private CompletableFuture<Observed> upload(FailingSource source) {
        CompletableFuture<Observed> observed = new CompletableFuture<>();
        multipart.client().putObject(r -> r.bucket("bucket").key("key"), source.body)
                .whenComplete((result, failure) -> observed.complete(
                        new Observed(Vertx.currentContext(), Thread.currentThread().getName(), failure)));
        source.release.complete(null);
        return observed;
    }

    @Test
    public void uploadSourceFailureCompletesTheFinalFutureOnTheCallerContext() throws Exception {
        Context caller = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        try (FailingSource source = new FailingSource()) {
            CompletableFuture<Observed> completed = new CompletableFuture<>();
            caller.runOnContext(ignored -> upload(source).thenAccept(completed::complete));
            Observed observed = completed.get(10, TimeUnit.SECONDS);
            assertSourceFailure(source, observed);
            if (enabled()) {
                assertSame(caller, observed.context());
            } else {
                assertNull(observed.context());
                assertEquals("multipart-source", observed.thread());
            }
        }
    }

    @Test
    public void uploadOutsideVertxFollowsTheConfiguredPolicy() throws Exception {
        try (FailingSource source = new FailingSource()) {
            Observed observed = upload(source).get(10, TimeUnit.SECONDS);
            assertSourceFailure(source, observed);
            assertNull(observed.context());
            assertEquals("multipart-source", observed.thread());
        }
    }

    @Test
    public void blockingWorkerDoesNotWaitForItsOwnCompletion() throws Exception {
        Context caller = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        try (FailingSource source = new FailingSource()) {
            Observed observed = caller.executeBlocking(() -> upload(source).get(5, TimeUnit.SECONDS))
                    .await(10, TimeUnit.SECONDS);
            assertSourceFailure(source, observed);
            assertNull(observed.context());
        }
    }

    private void assertSourceFailure(FailingSource source, Observed observed) {
        assertNotNull(observed.failure());
        Throwable failure = observed.failure();
        while (failure.getCause() != null) {
            failure = failure.getCause();
        }
        assertSame(source.failure, failure);
    }

    @Test
    public void cancellationStillReachesTheSdkCompositeFuture() throws Exception {
        Context caller = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        CompletableFuture<String> sdkComposite = new CompletableFuture<>();
        CompletableFuture<CompletableFuture<String>> request = new CompletableFuture<>();
        caller.runOnContext(ignored -> {
            SdkInternalCalls.enterComposite(multipart.client());
            request.complete(SdkInternalCalls.exitComposite(sdkComposite));
        });
        assertTrue(request.get(10, TimeUnit.SECONDS).cancel(true));
        assertTrue(sdkComposite.isCancelled());
    }

    @Test
    public void uploadDownloadAndCopyCompleteSuccessfully() throws Exception {
        Context caller = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        CompletableFuture<Void> completed = new CompletableFuture<>();
        caller.runOnContext(ignored -> multipart.client()
                .putObject(r -> r.bucket("bucket").key("key"), AsyncRequestBody.fromString("uploaded data"))
                .thenCompose(uploaded -> {
                    if (enabled()) {
                        assertSame(caller, Vertx.currentContext());
                    }
                    return multipart.client().getObject(r -> r.bucket("bucket").key("key"), AsyncResponseTransformer.toBytes());
                }).thenCompose(response -> {
                    assertEquals("multipart data", response.asUtf8String());
                    if (enabled()) {
                        assertSame(caller, Vertx.currentContext());
                    }
                    return multipart.client().copyObject(r -> r.sourceBucket("bucket").sourceKey("key")
                            .destinationBucket("bucket").destinationKey("copy"));
                }).thenAccept(response -> {
                    if (enabled()) {
                        assertSame(caller, Vertx.currentContext());
                    }
                }).whenComplete((response, failure) -> {
                    if (failure != null) {
                        completed.completeExceptionally(failure);
                    } else {
                        completed.complete(null);
                    }
                }));
        completed.get(20, TimeUnit.SECONDS);
    }

    @Test
    public void multipartUploadKeepsItsPartsOnSdkThreads() throws Exception {
        Context caller = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        int before = multipart.parts();
        CompletableFuture<Context> completed = new CompletableFuture<>();
        caller.runOnContext(ignored -> multipart.client()
                .putObject(r -> r.bucket("bucket").key("key"),
                        AsyncRequestBody.fromBytes(new byte[(int) (3 * MultipartClient.PART_SIZE)]))
                .whenComplete((response, failure) -> {
                    if (failure != null) {
                        completed.completeExceptionally(failure);
                    } else {
                        completed.complete(Vertx.currentContext());
                    }
                }));
        Context context = completed.get(20, TimeUnit.SECONDS);
        assertEquals(3, multipart.parts() - before);
        if (enabled()) {
            assertSame(caller, context);
        }
        // Only the composite result is dispatched, not the requests the SDK makes on its own behalf
        Map<String, Boolean> captured = multipart.captured();
        assertEquals(Map.of("CreateMultipartUploadRequest", false, "UploadPartRequest", false,
                "CompleteMultipartUploadRequest", false),
                Map.of(
                        "CreateMultipartUploadRequest", captured.get("CreateMultipartUploadRequest"),
                        "UploadPartRequest", captured.get("UploadPartRequest"),
                        "CompleteMultipartUploadRequest", captured.get("CompleteMultipartUploadRequest")));
    }

    @Test
    public void unmanagedMultipartClientKeepsItsSourceThread() throws Exception {
        try (S3AsyncClient unmanaged = S3AsyncClient.builder().multipartEnabled(true)
                .httpClientBuilder(NettyNioAsyncHttpClient.builder())
                .region(Region.US_EAST_1)
                .credentialsProvider(AnonymousCredentialsProvider.create()).build();
                FailingSource source = new FailingSource()) {
            Context caller = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
            CompletableFuture<Observed> completed = new CompletableFuture<>();
            caller.runOnContext(ignored -> {
                unmanaged.putObject(r -> r.bucket("bucket").key("key"), source.body).whenComplete((r, t) -> completed
                        .complete(new Observed(Vertx.currentContext(), Thread.currentThread().getName(), t)));
                source.release.complete(null);
            });
            Observed observed = completed.get(10, TimeUnit.SECONDS);
            assertSourceFailure(source, observed);
            assertNull(observed.context());
            assertEquals("multipart-source", observed.thread());
        }
    }

    static final class FailingSource implements AutoCloseable {
        final IllegalStateException failure = new IllegalStateException("source failed before any HTTP request");
        final CompletableFuture<Void> release = new CompletableFuture<>();
        final ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "multipart-source"));
        final AsyncRequestBody body = AsyncRequestBody.fromPublisher(subscriber -> subscriber.onSubscribe(new Subscription() {
            final AtomicBoolean requested = new AtomicBoolean();

            @Override
            public void request(long n) {
                if (requested.compareAndSet(false, true)) {
                    release.thenRunAsync(() -> subscriber.onError(failure), executor);
                }
            }

            @Override
            public void cancel() {
            }
        }));

        @Override
        public void close() {
            executor.shutdownNow();
        }
    }

    @ApplicationScoped
    public static class MultipartClient {
        static final long PART_SIZE = 1024;
        final AtomicInteger parts = new AtomicInteger();
        final CaptureProbeInterceptor probe = new CaptureProbeInterceptor();
        private final S3AsyncClient client;
        private final HttpServer server;

        @Inject
        public MultipartClient(S3AsyncClientBuilder builder) throws IOException {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                exchange.getRequestBody().readAllBytes();
                String query = exchange.getRequestURI().getQuery() == null ? "" : exchange.getRequestURI().getQuery();
                if (query.contains("partNumber") && exchange.getRequestMethod().equals("PUT")) {
                    parts.incrementAndGet();
                }
                if (exchange.getRequestMethod().equals("POST")) {
                    byte[] body = (query.contains("uploads")
                            ? "<InitiateMultipartUploadResult><Bucket>bucket</Bucket><Key>key</Key>"
                                    + "<UploadId>upload</UploadId></InitiateMultipartUploadResult>"
                            : "<CompleteMultipartUploadResult><Bucket>bucket</Bucket><Key>key</Key>"
                                    + "<ETag>\"completed\"</ETag></CompleteMultipartUploadResult>")
                            .getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                    return;
                }
                byte[] body = (exchange.getRequestHeaders().containsKey("x-amz-copy-source")
                        ? "<CopyObjectResult><ETag>\"copied\"</ETag><LastModified>2026-01-01T00:00:00Z</LastModified></CopyObjectResult>"
                        : exchange.getRequestMethod().equals("PUT") ? "" : "multipart data").getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Length", Integer.toString(body.length));
                exchange.getResponseHeaders().set("ETag", "\"test-etag\"");
                if (exchange.getRequestMethod().equals("HEAD") || body.length == 0) {
                    exchange.sendResponseHeaders(200, -1);
                } else {
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                }
                exchange.close();
            });
            server.start();
            builder.overrideConfiguration(builder.overrideConfiguration().toBuilder().addExecutionInterceptor(probe).build());
            client = builder.multipartEnabled(true)
                    .multipartConfiguration(c -> c.thresholdInBytes(PART_SIZE).minimumPartSizeInBytes(PART_SIZE))
                    .endpointOverride(URI.create("http://localhost:" + server.getAddress().getPort()))
                    .build();
        }

        public S3AsyncClient client() {
            return client;
        }

        public int parts() {
            return parts.get();
        }

        public Map<String, Boolean> captured() {
            return Map.copyOf(probe.captured);
        }

        @PreDestroy
        void close() {
            client.close();
            server.stop(0);
        }
    }
}
