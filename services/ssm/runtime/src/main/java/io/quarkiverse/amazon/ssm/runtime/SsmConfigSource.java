package io.quarkiverse.amazon.ssm.runtime;

import static software.amazon.awssdk.utils.StringUtils.isBlank;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.logging.Logger;

import io.quarkiverse.amazon.common.runtime.ConfigSourceFailure;
import io.quarkiverse.amazon.common.runtime.ConfigSourceFormat;
import io.smallrye.config.common.AbstractConfigSource;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersRequest;
import software.amazon.awssdk.services.ssm.model.Parameter;

/**
 * MicroProfile {@link org.eclipse.microprofile.config.spi.ConfigSource} backed by AWS Systems Manager Parameter Store.
 * <p>
 * Loads parameters by path ({@code GetParametersByPath}) and/or explicit names ({@code GetParameters}), maps each
 * parameter value into one or more configuration keys, then optionally reloads on a fixed interval.
 * <p>
 * Parameter names are normalized into MicroProfile-friendly keys by stripping a leading {@code '/'} and replacing
 * remaining slashes with {@code '.'} (for example {@code /myapp/prod/url} becomes {@code myapp.prod.url}).
 * <p>
 * A parameter value that is JSON (object or array) is flattened like the AppConfig Data config source; inner keys are
 * prefixed with the normalized parameter name. Other values are exposed as a single key (the normalized name).
 * Java properties documents require an explicit format.
 * <p>
 * The supplied {@link SsmClient} is owned by this source. Without polling, it is closed right after the initial load;
 * otherwise {@link #close()} closes it, which {@link SsmConfigSourceCloser} calls on shutdown.
 */
public class SsmConfigSource extends AbstractConfigSource implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(SsmConfigSource.class);
    private static final String CONFIG_SOURCE_NAME = "quarkus.ssm.config";

    private static final String POLL_THREAD_NAME = "quarkus-amazon-ssm-config-poll";

    /** AWS allows at most ten names per {@link GetParametersRequest}. */
    private static final int GET_PARAMETERS_BATCH_SIZE = 10;

    private final AtomicReference<Map<String, String>> entriesRef = new AtomicReference<>(Map.of());
    private final AtomicBoolean closed = new AtomicBoolean();
    private final SsmClient client;
    private final String path;
    private final List<String> names;
    private final boolean recursive;
    private final boolean withDecryption;
    private final ConfigSourceFormat format;

    private ScheduledExecutorService executorService;

    public SsmConfigSource(SsmClient client, String path, List<String> names, boolean recursive, boolean withDecryption,
            Duration updateInterval, int ordinal) {
        this(client, path, names, recursive, withDecryption, updateInterval, ordinal, ConfigSourceFormat.AUTO);
    }

    public SsmConfigSource(SsmClient client, String path, List<String> names, boolean recursive, boolean withDecryption,
            Duration updateInterval, int ordinal, ConfigSourceFormat format) {
        super(CONFIG_SOURCE_NAME, ordinal);
        this.client = client;
        this.path = path;
        this.names = names != null ? List.copyOf(names) : List.of();
        this.recursive = recursive;
        this.withDecryption = withDecryption;
        this.format = format;

        reload();

        if (updateInterval.toSeconds() > 0L) {
            LOG.debugf("SSM ConfigSource update interval set to %s", updateInterval);
            ThreadFactory threadFactory = runnable -> {
                Thread thread = new Thread(runnable, POLL_THREAD_NAME);
                thread.setDaemon(true);
                return thread;
            };
            executorService = new ScheduledThreadPoolExecutor(1, threadFactory);
            executorService.scheduleAtFixedRate(this::reload, updateInterval.toSeconds(), updateInterval.toSeconds(),
                    TimeUnit.SECONDS);
        } else {
            // nothing will use the client again
            close();
        }
    }

    void reload() {
        if (closed.get()) {
            return;
        }
        try {
            Map<String, String> merged = new LinkedHashMap<>();
            if (!isBlank(path)) {
                mergeParametersByPath(merged, path.trim());
            }
            for (List<String> batch : partition(names, GET_PARAMETERS_BATCH_SIZE)) {
                if (!mergeParametersByName(merged, batch)) {
                    return;
                }
            }
            entriesRef.set(Collections.unmodifiableMap(merged));
            LOG.debugf("SSM ConfigSource now exposes %d configuration entries.", merged.size());
        } catch (Exception e) {
            ConfigSourceFailure.classify(e).log(LOG, "Unable to reload SSM configuration; keeping the previous snapshot");
        }
    }

    private void mergeParametersByPath(Map<String, String> target, String pathValue) {
        String nextToken = null;
        do {
            var reqBuilder = GetParametersByPathRequest.builder()
                    .path(normalizePath(pathValue))
                    .recursive(recursive)
                    .withDecryption(withDecryption);
            if (nextToken != null) {
                reqBuilder.nextToken(nextToken);
            }
            var response = client.getParametersByPath(reqBuilder.build());
            nextToken = response.nextToken();
            for (Parameter p : response.parameters()) {
                mergeParameter(target, p.name(), p.value());
            }
        } while (nextToken != null && !nextToken.isEmpty());
    }

    private boolean mergeParametersByName(Map<String, String> target, List<String> batch) {
        if (batch.isEmpty()) {
            return true;
        }
        var response = client.getParameters(GetParametersRequest.builder()
                .names(batch)
                .withDecryption(withDecryption)
                .build());
        if (!response.invalidParameters().isEmpty()) {
            ConfigSourceFailure.PARTIAL_RESPONSE.log(LOG,
                    "Some configured SSM parameters are missing; keeping the previous snapshot");
            return false;
        }
        for (Parameter p : response.parameters()) {
            mergeParameter(target, p.name(), p.value());
        }
        return true;
    }

    private void mergeParameter(Map<String, String> target, String awsName, String value) {
        target.putAll(format.decode(normalizeParameterName(awsName), value));
    }

    /**
     * Turns a Parameter Store name into a configuration key prefix (no leading slash, {@code '/'} to {@code '.'}).
     */
    static String normalizeParameterName(String name) {
        if (name == null) {
            return "";
        }
        String n = name.trim();
        if (n.startsWith("/")) {
            n = n.substring(1);
        }
        return n.replace('/', '.');
    }

    static String normalizePath(String pathValue) {
        if (pathValue == null || pathValue.isEmpty()) {
            return "/";
        }
        return pathValue.startsWith("/") ? pathValue : "/" + pathValue;
    }

    /**
     * Maps one parameter's value to zero or more configuration keys under {@link #normalizeParameterName(String)}.
     */
    static Map<String, String> expandParameterValue(String awsName, String value) {
        return ConfigSourceFormat.AUTO.decode(normalizeParameterName(awsName), value);
    }

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> parts = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            parts.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return parts;
    }

    private Map<String, String> rawEntries() {
        return entriesRef.get();
    }

    @Override
    public Set<String> getPropertyNames() {
        return rawEntries().keySet();
    }

    @Override
    public String getValue(String propertyName) {
        return rawEntries().get(propertyName);
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        client.close();
    }
}
