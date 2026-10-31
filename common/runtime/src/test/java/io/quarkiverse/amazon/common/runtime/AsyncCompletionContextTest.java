package io.quarkiverse.amazon.common.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;

class AsyncCompletionContextTest {
    @Test
    void leavesUnconfiguredClientsAndWorkerCallsUntouched() {
        CompletableFuture<String> source = new CompletableFuture<>();
        assertSame(source, AsyncCompletionContext.complete(source, new ExecutionAttributes()));
    }

    @Test
    void isolatesDispatchersBetweenRequests() {
        AtomicInteger first = new AtomicInteger();
        AtomicInteger second = new AtomicInteger();
        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(AsyncCompletionContext.EXECUTOR, command -> {
            first.incrementAndGet();
            command.run();
        });
        ExecutionAttributes other = new ExecutionAttributes();
        other.putAttribute(AsyncCompletionContext.EXECUTOR, command -> {
            second.incrementAndGet();
            command.run();
        });
        CompletableFuture<String> one = new CompletableFuture<>();
        CompletableFuture<String> resultOne = AsyncCompletionContext.complete(one, attributes);
        one.complete("one");
        assertEquals("one", resultOne.join());
        assertEquals(1, first.get());
        assertEquals(0, second.get());
        CompletableFuture<String> two = new CompletableFuture<>();
        CompletableFuture<String> resultTwo = AsyncCompletionContext.complete(two, other);
        two.complete("two");
        assertEquals("two", resultTwo.join());
        assertEquals(1, first.get());
        assertEquals(1, second.get());
    }

    @Test
    void forwardsCancellationToTheSdkFuture() {
        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(AsyncCompletionContext.EXECUTOR, Runnable::run);
        CompletableFuture<String> source = new CompletableFuture<>();
        CompletableFuture<String> result = AsyncCompletionContext.complete(source, attributes);
        result.cancel(true);
        assertTrue(source.isCancelled());
    }

    @Test
    void forwardsSdkCancellationToTheReturnedFuture() {
        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(AsyncCompletionContext.EXECUTOR, Runnable::run);
        CompletableFuture<String> source = new CompletableFuture<>();
        CompletableFuture<String> result = AsyncCompletionContext.complete(source, attributes);
        source.cancel(true);
        assertTrue(result.isCancelled());
    }

    @Test
    void forwardsCallerTimeoutsAndOtherExceptionalCompletions() {
        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(AsyncCompletionContext.EXECUTOR, Runnable::run);
        CompletableFuture<String> source = new CompletableFuture<>();
        CompletableFuture<String> result = AsyncCompletionContext.complete(source, attributes);
        var timeout = new java.util.concurrent.TimeoutException("caller deadline");
        result.completeExceptionally(timeout);
        assertSame(timeout, assertThrows(CompletionException.class, source::join).getCause());
    }

    @Test
    void rejectionDoesNotLoseTheOriginalResultOrFailure() {
        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(AsyncCompletionContext.EXECUTOR, command -> {
            throw new RejectedExecutionException("closed");
        });
        CompletableFuture<String> success = new CompletableFuture<>();
        CompletableFuture<String> successResult = AsyncCompletionContext.complete(success, attributes);
        success.complete("ok");
        assertEquals("ok", successResult.join());
        IllegalStateException failure = new IllegalStateException("original");
        CompletableFuture<String> failing = new CompletableFuture<>();
        CompletableFuture<String> failingResult = AsyncCompletionContext.complete(failing, attributes);
        failing.completeExceptionally(failure);
        assertSame(failure, assertThrows(CompletionException.class, failingResult::join).getCause());
    }

    @Test
    void unexpectedDispatchFailureStillDeliversTheResult() {
        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(AsyncCompletionContext.EXECUTOR, command -> {
            throw new IllegalStateException("dispatch");
        });
        CompletableFuture<String> source = new CompletableFuture<>();
        CompletableFuture<String> result = AsyncCompletionContext.complete(source, attributes);
        source.complete("ok");
        assertEquals("ok", result.join());
    }

    @Test
    void closesAResultNobodyAwaitsAnymore() {
        java.util.List<Runnable> queued = new java.util.ArrayList<>();
        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(AsyncCompletionContext.EXECUTOR, queued::add);
        CompletableFuture<AutoCloseable> source = new CompletableFuture<>();
        CompletableFuture<AutoCloseable> result = AsyncCompletionContext.complete(source, attributes);
        AtomicInteger closed = new AtomicInteger();
        // The SDK produced a response, e.g. holding a connection, but the caller times out before it is dispatched
        source.complete(closed::incrementAndGet);
        result.completeExceptionally(new java.util.concurrent.TimeoutException());
        queued.forEach(Runnable::run);
        assertEquals(1, closed.get());
    }

    @Test
    void cancelsAPublisherNobodyAwaitsAnymore() {
        java.util.List<Runnable> queued = new java.util.ArrayList<>();
        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(AsyncCompletionContext.EXECUTOR, queued::add);
        CompletableFuture<software.amazon.awssdk.core.async.SdkPublisher<String>> source = new CompletableFuture<>();
        CompletableFuture<software.amazon.awssdk.core.async.SdkPublisher<String>> result = AsyncCompletionContext
                .complete(source, attributes);
        AtomicInteger cancelled = new AtomicInteger();
        source.complete(subscriber -> subscriber.onSubscribe(new org.reactivestreams.Subscription() {
            @Override
            public void request(long n) {
            }

            @Override
            public void cancel() {
                cancelled.incrementAndGet();
            }
        }));
        result.completeExceptionally(new java.util.concurrent.TimeoutException());
        queued.forEach(Runnable::run);
        assertEquals(1, cancelled.get());
    }

    @Test
    void doesNotCloseADeliveredResult() {
        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(AsyncCompletionContext.EXECUTOR, Runnable::run);
        AtomicInteger closed = new AtomicInteger();
        AutoCloseable response = closed::incrementAndGet;
        CompletableFuture<AutoCloseable> source = new CompletableFuture<>();
        CompletableFuture<AutoCloseable> result = AsyncCompletionContext.complete(source, attributes);
        source.complete(response);
        assertSame(response, result.join());
        assertEquals(0, closed.get());
    }

    @Test
    void internalSdkCallsAreNotCaptured() {
        ExecutionAttributes attributes = new ExecutionAttributes();
        VertxContextCapturingInterceptor interceptor = new VertxContextCapturingInterceptor(true);
        SdkInternalCalls.enterComposite(new SdkClient() {
            @Override
            public String serviceName() {
                return "test";
            }

            @Override
            public void close() {
            }
        });
        try {
            assertTrue(SdkInternalCalls.active());
            interceptor.beforeExecution(null, attributes);
        } finally {
            SdkInternalCalls.abortComposite();
        }
        assertNull(attributes.getAttribute(AsyncCompletionContext.EXECUTOR));
        assertFalse(SdkInternalCalls.active());
    }

    @Test
    void keepsSynchronousFailuresCompletedOnTheCallerThread() {
        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(AsyncCompletionContext.EXECUTOR, command -> {
            throw new AssertionError("Must not dispatch");
        });
        CompletableFuture<String> failed = CompletableFuture.failedFuture(new IllegalArgumentException("invalid"));
        assertSame(failed, AsyncCompletionContext.complete(failed, attributes));
    }
}
