package io.quarkiverse.amazon.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.quarkiverse.amazon.common.runtime.ConfigSourceFormat;

class AbstractAwsConfigSourceTest {
    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = { "PT-1S", "PT0.5S" })
    void invalidIntervalsFailBeforeFetchingAndCloseTheOwnedProvider(String value) {
        var provider = new StubProvider(result("value", Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new Source(provider, Duration.parse(value)));
        assertEquals(0, provider.calls);
        assertEquals(1, provider.closed);
    }

    private final ResourceId id = new ResourceId("target");

    @Test
    void startupOnlyClosesProviderOnceAndPreservesOrdinal() {
        var provider = new StubProvider(result("value", Duration.ZERO));
        try (var source = new Source(provider, Duration.ZERO)) {
            assertEquals(1, provider.closed);
            assertEquals("value", source.getValue("target"));
            assertEquals(275, source.getOrdinal());
            source.refresh();
            assertEquals(1, provider.calls);
        }
        assertEquals(1, provider.closed);
    }

    @Test
    void pollsNoFasterThanEitherConfiguredOrProviderMinimum() {
        var provider = new StubProvider(result("old", Duration.ofSeconds(90)),
                new FetchResult(Map.of(), Set.of(), Set.of(), Map.of(id, FetchFailure.TRANSIENT), null, Duration.ofSeconds(90)),
                result("new", Duration.ofSeconds(60)));
        try (var source = new Source(provider, Duration.ofSeconds(2))) {
            assertEquals(Duration.ofSeconds(90), source.pollDelay());
            source.refresh();
            assertEquals("old", source.getValue("target"));
            assertEquals(Duration.ofSeconds(90), source.pollDelay());
            source.refresh();
            assertEquals("new", source.getValue("target"));
            assertEquals(Duration.ofSeconds(60), source.pollDelay());
        }
        try (var source = new Source(new StubProvider(result("value", Duration.ofSeconds(60))), Duration.ofHours(1))) {
            assertEquals(Duration.ofHours(1), source.pollDelay());
        }
    }

    @Test
    void strictTextEncodingNeverReplacesInvalidSurrogates() {
        assertThrows(IllegalArgumentException.class, () -> RawDocument.text(id, "\uD800", null, null));
        var document = RawDocument.text(id, "été", null, null);
        assertEquals(Map.of("target", "été"),
                new ConfigSourceEntry("target", id, "target", ConfigSourceFormat.RAW, false).decode(document));
    }

    @ParameterizedTest
    @EnumSource(FetchFailure.class)
    void requiredStartupRejectsBothResourceAndBatchFailuresAndClosesProvider(FetchFailure failure) {
        for (boolean batch : List.of(false, true)) {
            var provider = new StubProvider(new FetchResult(Map.of(), Set.of(), Set.of(),
                    batch ? Map.of() : Map.of(id, failure), batch ? failure : null, Duration.ZERO));
            var error = assertThrows(IllegalStateException.class,
                    () -> new Source(provider, Duration.ofHours(1), true));
            assertEquals("Unable to load test [" + failure + "]", error.getMessage());
            assertNull(error.getCause());
            assertEquals(1, provider.calls);
            assertEquals(1, provider.closed);
        }
    }

    @Test
    void optionalStartupCanRecoverAndRequiredRefreshStillPreservesSnapshot() {
        var missing = new FetchResult(Map.of(), Set.of(), Set.of(), Map.of(id, FetchFailure.MISSING), null, Duration.ZERO);
        try (var source = new Source(new StubProvider(missing, result("recovered", Duration.ZERO)), Duration.ofHours(1),
                false)) {
            assertTrue(source.getPropertyNames().isEmpty());
            source.refresh();
            assertEquals("recovered", source.getValue("target"));
        }
        try (var source = new Source(new StubProvider(result("old", Duration.ZERO), missing), Duration.ofHours(1), true)) {
            source.refresh();
            assertEquals("old", source.getValue("target"));
        }
    }

    @Test
    void invalidUtf8FailsRequiredStartupWithoutLeakingContent() {
        var provider = new StubProvider(new FetchResult(Map.of(id, new RawDocument(id, new byte[] { (byte) 0xff }, null, null)),
                Set.of(), Set.of(), Map.of(), null, Duration.ZERO));
        var error = assertThrows(IllegalStateException.class, () -> new Source(provider, Duration.ZERO, true));
        assertEquals("Unable to load test [INVALID_CONTENT]", error.getMessage());
        assertNull(error.getCause());
        assertEquals(1, provider.closed);
    }

    @Test
    void unexpectedFetchAndCloseErrorsCannotLeakThroughStartupException() {
        var error = assertThrows(IllegalStateException.class, () -> new Source(new AwsConfigProvider() {
            public FetchResult fetchAll() {
                throw new IllegalStateException("unsafe token");
            }

            public void close() {
                throw new IllegalStateException("unsafe secret");
            }
        }, Duration.ZERO, true));
        assertEquals("Unable to load test [OTHER]", error.getMessage());
        assertNull(error.getCause());
        assertEquals(0, error.getSuppressed().length);
    }

    @Test
    void scheduledPollingContinuesAfterFailureAndStopsOnClose() throws InterruptedException {
        var recovered = new CountDownLatch(1);
        var unexpected = new CountDownLatch(1);
        var calls = new AtomicInteger();
        var closes = new AtomicInteger();
        AwsConfigProvider provider = new AwsConfigProvider() {
            public FetchResult fetchAll() {
                if (closes.get() > 0) {
                    unexpected.countDown();
                }
                int call = calls.incrementAndGet();
                if (call == 2) {
                    return new FetchResult(Map.of(), Set.of(), Set.of(), Map.of(id, FetchFailure.TRANSIENT), null,
                            Duration.ZERO);
                }
                if (call == 3) {
                    recovered.countDown();
                }
                return result("value", Duration.ZERO);
            }

            public void close() {
                closes.incrementAndGet();
            }
        };
        try (var source = new Source(provider, Duration.ofSeconds(1))) {
            assertTrue(recovered.await(10, TimeUnit.SECONDS), "Polling stopped after the failed fetch");
        }
        assertEquals(1, closes.get());
        int callsAtClose = calls.get();
        assertFalse(unexpected.await(1100, TimeUnit.MILLISECONDS), "Polling continued after close");
        assertEquals(callsAtClose, calls.get());
    }

    @Test
    void sharedSourceLogsNoExceptionMessagesOrCauses() {
        var records = new ArrayList<LogRecord>();
        var jul = java.util.logging.Logger.getLogger(AbstractAwsConfigSource.class.getName());
        Handler handler = new Handler() {
            public void publish(LogRecord record) {
                records.add(record);
            }

            public void flush() {
            }

            public void close() {
            }
        };
        jul.addHandler(handler);
        try (var source = new Source(new AwsConfigProvider() {
            public FetchResult fetchAll() {
                throw new IllegalStateException("unsafe token and secret");
            }

            public void close() {
            }
        }, Duration.ZERO)) {
            assertTrue(source.getPropertyNames().isEmpty());
            assertEquals(1, records.size());
            assertEquals("Unable to update test; keeping the previous snapshot [OTHER]", records.getFirst().getMessage());
            assertNull(records.getFirst().getThrown());
        } finally {
            jul.removeHandler(handler);
        }
    }

    private FetchResult result(String value, Duration delay) {
        return new FetchResult(Map.of(id, RawDocument.text(id, value, null, null)), Set.of(), Set.of(), Map.of(), null, delay);
    }

    private final class Source extends AbstractAwsConfigSource {
        Source(AwsConfigProvider provider, Duration interval) {
            this(provider, interval, false);
        }

        Source(AwsConfigProvider provider, Duration interval, boolean required) {
            super("test", 275, provider,
                    key -> List.of(new ConfigSourceEntry("target", key, "target", ConfigSourceFormat.RAW, false)),
                    interval, "test-config-poll", required);
        }
    }

    private static final class StubProvider implements AwsConfigProvider {
        final ArrayDeque<FetchResult> results;
        int closed;
        int calls;

        StubProvider(FetchResult... results) {
            this.results = new ArrayDeque<>(List.of(results));
        }

        public FetchResult fetchAll() {
            assertTrue(closed == 0);
            calls++;
            return results.size() > 1 ? results.removeFirst() : results.getFirst();
        }

        public void close() {
            closed++;
        }
    }
}
