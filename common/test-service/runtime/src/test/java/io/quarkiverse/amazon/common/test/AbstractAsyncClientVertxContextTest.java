package io.quarkiverse.amazon.common.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;

public abstract class AbstractAsyncClientVertxContextTest {

    @Inject
    Vertx vertx;

    protected abstract AwsClient client();

    protected abstract AwsClient namedClient();

    protected abstract AwsAsyncClientBuilder<?, ?> configuredBuilder();

    protected abstract AwsAsyncClientBuilder<?, ?> newBuilder();

    protected abstract AwsAsyncClientBuilder<?, ?> customBuilder();

    protected abstract CompletableFuture<?> call(AwsClient client);

    @Test
    public void blockingEndpointUsesTheDocumentedPolicyOnRealQuarkusWorkers() {
        given().get("/async-context/blocking").then().statusCode(200)
                .body(is("true|false|false"));
    }

    @Test
    public void virtualThreadEndpointUsesTheDocumentedPolicy() {
        given().get("/async-context/virtual").then().statusCode(200)
                .body(is("true|false|false"));
    }

    @Test
    public void respectsTheExecutorConfiguredOnTheProvidedBuilder() throws Exception {
        AtomicInteger executions = new AtomicInteger();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        // The SDK builder cannot restore its async configuration: only this test uses this builder
        AwsAsyncClientBuilder<?, ?> builder = customBuilder();
        builder.asyncConfiguration(config -> config.advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR,
                command -> {
                    executions.incrementAndGet();
                    executor.execute(command);
                }));
        try (AwsClient local = (AwsClient) ((AwsClientBuilder<?, ?>) builder).build()) {
            call(local).handle((response, failure) -> null).get(30, TimeUnit.SECONDS);
            assertTrue(executions.get() > 0, "The SDK must use the application's completion executor");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void replacingTheOverrideConfigurationKeepsPropagation() throws Exception {
        CallerContextProbeInterceptor.CAPTURED.set(null);
        AwsClientBuilder<?, ?> builder = (AwsClientBuilder<?, ?>) configuredBuilder();
        ClientOverrideConfiguration original = builder.overrideConfiguration();
        // Replaces the whole override configuration provided by the extension
        builder.overrideConfiguration(c -> c.addExecutionInterceptor(new CallerContextProbeInterceptor()));
        try (AwsClient local = (AwsClient) builder.build()) {
            Context duplicated = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
            CompletableFuture<Context> completionContext = new CompletableFuture<>();
            duplicated.runOnContext(ignored -> call(local)
                    .whenComplete((response, failure) -> completionContext.complete(Vertx.currentContext())));
            assertSame(duplicated, completionContext.get(30, TimeUnit.SECONDS));
        } finally {
            builder.overrideConfiguration(original);
        }
        assertTrue(CallerContextProbeInterceptor.CAPTURED.get());
    }

    @Test
    public void apiCallTimeoutCompletesOnCallerContext() throws Exception {
        checkTimeoutContext(false);
    }

    @Test
    public void attemptTimeoutCompletesOnCallerContext() throws Exception {
        checkTimeoutContext(true);
    }

    private void checkTimeoutContext(boolean attempt) throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            try {
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        AwsClientBuilder<?, ?> builder = (AwsClientBuilder<?, ?>) configuredBuilder();
        URI endpoint = client().serviceClientConfiguration().endpointOverride().orElseThrow();
        ClientOverrideConfiguration original = builder.overrideConfiguration();
        server.start();
        try {
            var overrides = original.toBuilder().retryStrategy(AwsRetryStrategy.doNotRetry());
            if (attempt)
                overrides.apiCallAttemptTimeout(Duration.ofMillis(500));
            else
                overrides.apiCallTimeout(Duration.ofMillis(500));
            builder.overrideConfiguration(overrides.build());
            builder.endpointOverride(URI.create("http://localhost:" + server.getAddress().getPort()));
            try (AwsClient local = (AwsClient) builder.build()) {
                Context caller = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
                CompletableFuture<Throwable> completed = new CompletableFuture<>();
                caller.runOnContext(ignored -> call(local).whenComplete((response, failure) -> {
                    if (Vertx.currentContext() != caller)
                        completed.completeExceptionally(new AssertionError("Timeout lost caller context"));
                    else
                        completed.complete(failure);
                }));
                Throwable failure = completed.get(10, TimeUnit.SECONDS);
                while (failure.getCause() != null)
                    failure = failure.getCause();
                if (attempt)
                    assertInstanceOf(ApiCallAttemptTimeoutException.class, failure);
                else
                    assertInstanceOf(ApiCallTimeoutException.class, failure);
            }
        } finally {
            builder.endpointOverride(endpoint);
            builder.overrideConfiguration(original);
            release.countDown();
            server.stop(0);
        }
    }

    @Test
    public void completesSuccessfulResponseOnCallerContext() throws Exception {
        checkResponseContext(false);
    }

    @Test
    public void retainsCallerContextAcrossRetry() throws Exception {
        checkResponseContext(true);
    }

    private void checkResponseContext(boolean retry) throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            boolean fail = attempts.incrementAndGet() == 1 && retry;
            byte[] body = (fail ? "{\"__type\":\"InternalServerError\"}" : "{}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/x-amz-json-1.0");
            exchange.sendResponseHeaders(fail ? 500 : 200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        AwsClientBuilder<?, ?> builder = (AwsClientBuilder<?, ?>) configuredBuilder();
        URI endpoint = client().serviceClientConfiguration().endpointOverride().orElseThrow();
        try (AwsClient local = (AwsClient) builder
                .endpointOverride(URI.create("http://localhost:" + server.getAddress().getPort()))
                .build()) {
            Context duplicated = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
            CompletableFuture<Context> completion = new CompletableFuture<>();
            duplicated.runOnContext(ignored -> call(local).whenComplete((response, failure) -> {
                if (failure != null) {
                    completion.completeExceptionally(failure);
                } else {
                    completion.complete(Vertx.currentContext());
                }
            }));
            assertSame(duplicated, completion.get(30, TimeUnit.SECONDS));
            assertEquals(retry ? 2 : 1, attempts.get());
        } finally {
            builder.endpointOverride(endpoint);
            server.stop(0);
        }
    }

    @Test
    public void completesOnCallerDuplicatedContext() throws Exception {
        assertCallerContext(client());
    }

    @Test
    public void completesNamedClientOnCallerDuplicatedContext() throws Exception {
        assertCallerContext(namedClient());
    }

    private void assertCallerContext(AwsClient client) throws Exception {
        CallerContextProbeInterceptor.CAPTURED.set(null);
        Context duplicated = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        CompletableFuture<Thread> callerThread = new CompletableFuture<>();
        CompletableFuture<Thread> completionThread = new CompletableFuture<>();
        CompletableFuture<Context> completionContext = new CompletableFuture<>();
        CompletableFuture<Throwable> failure = new CompletableFuture<>();

        // nothing listens on the endpoint, the call fails on an SDK thread
        duplicated.runOnContext(ignored -> {
            callerThread.complete(Thread.currentThread());
            call(client).whenComplete((r, t) -> {
                completionThread.complete(Thread.currentThread());
                completionContext.complete(Vertx.currentContext());
                failure.complete(t);
            });
        });

        assertNotNull(failure.get(30, TimeUnit.SECONDS));
        assertSame(duplicated, completionContext.get());
        assertSame(callerThread.get(), completionThread.get());
        // the interceptor capturing the context is registered along with the configured interceptors
        assertTrue(CallerContextProbeInterceptor.CAPTURED.get());
    }

    @Test
    public void completesOutsideOfVertxContext() throws Exception {
        CompletableFuture<Context> completionContext = new CompletableFuture<>();
        CompletableFuture<Throwable> failure = new CompletableFuture<>();

        call(client()).whenComplete((r, t) -> {
            completionContext.complete(Vertx.currentContext());
            failure.complete(t);
        });

        assertNotNull(failure.get(30, TimeUnit.SECONDS));
        Context context = completionContext.get();
        assertNull(context);
    }

    @Test
    public void doesNotAffectClientsBuiltByTheApplication() throws Exception {
        CallerContextProbeInterceptor.CAPTURED.set(null);
        Context duplicated = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        CompletableFuture<Context> completionContext = new CompletableFuture<>();
        CompletableFuture<Throwable> failure = new CompletableFuture<>();

        try (AwsClient handBuilt = (AwsClient) ((AwsClientBuilder<?, ?>) newBuilder()
                .httpClientBuilder(NettyNioAsyncHttpClient.builder()))
                .endpointOverride(URI.create("http://localhost:1"))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test-key", "test-secret")))
                .overrideConfiguration(c -> c.addExecutionInterceptor(new CallerContextProbeInterceptor()))
                .build()) {
            duplicated.runOnContext(ignored -> call(handBuilt).whenComplete((r, t) -> {
                completionContext.complete(Vertx.currentContext());
                failure.complete(t);
            }));

            assertNotNull(failure.get(30, TimeUnit.SECONDS));
        }
        assertNull(completionContext.get());
        // the interceptor capturing the context is not registered as a global SDK interceptor
        assertFalse(CallerContextProbeInterceptor.CAPTURED.get());
    }
}
