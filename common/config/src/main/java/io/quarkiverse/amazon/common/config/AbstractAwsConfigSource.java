package io.quarkiverse.amazon.common.config;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.jboss.logging.Logger;

import io.smallrye.config.common.AbstractConfigSource;

/** Shared publication, polling and ownership of a service provider. */
public abstract class AbstractAwsConfigSource extends AbstractConfigSource implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(AbstractAwsConfigSource.class);
    private final AwsConfigProvider provider;
    private final Function<ResourceId, List<ConfigSourceEntry>> entries;
    private final AwsConfigSnapshot snapshot = new AwsConfigSnapshot();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Duration updateInterval;
    private Duration minDelay = Duration.ZERO;
    private ScheduledThreadPoolExecutor executor;

    protected AbstractAwsConfigSource(String name, int ordinal, AwsConfigProvider provider,
            Function<ResourceId, List<ConfigSourceEntry>> entries, Duration updateInterval, String threadName) {
        super(name, ordinal);
        this.provider = provider;
        this.entries = entries;
        this.updateInterval = updateInterval;
        try {
            validateUpdateInterval(updateInterval);
            refresh();
            if (updateInterval.toSeconds() > 0) {
                // Runtime configuration is built before Quarkus initializes its managed executor.
                executor = new ScheduledThreadPoolExecutor(1, task -> {
                    Thread thread = new Thread(task, threadName);
                    thread.setDaemon(true);
                    return thread;
                });
                executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
                scheduleNext();
            } else {
                close();
            }
        } catch (RuntimeException e) {
            close();
            throw e;
        }
    }

    public static void validateUpdateInterval(Duration interval) {
        if (interval.isNegative() || !interval.isZero() && interval.compareTo(Duration.ofSeconds(1)) < 0) {
            throw new IllegalArgumentException("AWS configuration update-interval must be zero or at least one second.");
        }
    }

    protected final synchronized void refresh() {
        if (closed.get()) {
            return;
        }
        try {
            FetchResult result = provider.fetchAll();
            minDelay = result.minDelayBeforeNextFetch();
            if (!closed.get() && !snapshot.apply(result, entries)) {
                FetchFailure reason = result.batchFailure() != null ? result.batchFailure()
                        : result.failed().values().iterator().next();
                logFailure(reason);
            }
        } catch (ConfigSourceCollisionException e) {
            LOG.errorf("Unable to update %s; keeping the previous snapshot [INVALID_CONTENT]: %s", getName(), e.getMessage());
        } catch (IllegalArgumentException e) {
            logFailure(FetchFailure.INVALID_CONTENT);
        } catch (Exception e) {
            logFailure(FetchFailure.OTHER);
        }
    }

    protected final synchronized Duration pollDelay() {
        return updateInterval.compareTo(minDelay) >= 0 ? updateInterval : minDelay;
    }

    private void poll() {
        try {
            refresh();
        } finally {
            scheduleNext();
        }
    }

    private void scheduleNext() {
        if (!closed.get()) {
            try {
                executor.schedule(this::poll, pollDelay().toNanos(), TimeUnit.NANOSECONDS);
            } catch (RejectedExecutionException e) {
                if (!closed.get()) {
                    throw e;
                }
            }
        }
    }

    private void logFailure(FetchFailure reason) {
        if (reason == FetchFailure.TRANSIENT || reason == FetchFailure.MISSING || reason == FetchFailure.PARTIAL_RESPONSE) {
            LOG.warnf("Unable to update %s; keeping the previous snapshot [%s]", getName(), reason);
        } else {
            LOG.errorf("Unable to update %s; keeping the previous snapshot [%s]", getName(), reason);
        }
    }

    @Override
    public Set<String> getPropertyNames() {
        return snapshot.properties().keySet();
    }

    @Override
    public String getValue(String propertyName) {
        return snapshot.properties().get(propertyName);
    }

    @Override
    public final void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        provider.close();
    }
}
