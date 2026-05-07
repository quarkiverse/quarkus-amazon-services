package io.quarkiverse.amazon.ssm.runtime;

import static software.amazon.awssdk.utils.StringUtils.isBlank;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.logging.Logger;

import io.quarkiverse.amazon.common.runtime.JsonConfigFlattener;
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
 * prefixed with the normalized parameter name. Other values are exposed as a single key (the normalized name) unless
 * the value looks like Java properties text (contains an equals sign), in which case each entry is prefixed.
 * <p>
 * The supplied {@link SsmClient} is owned by this source and closed in {@link #close()}.
 */
public class SsmConfigSource extends AbstractConfigSource implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(SsmConfigSource.class);
    private static final String CONFIG_SOURCE_NAME = "quarkus.ssm.config";

    private static final String POLL_THREAD_NAME = "quarkus-amazon-ssm-config-poll";

    /** AWS allows at most ten names per {@link GetParametersRequest}. */
    private static final int GET_PARAMETERS_BATCH_SIZE = 10;

    private final AtomicReference<Map<String, String>> entriesRef = new AtomicReference<>(Map.of());
    private final SsmClient client;
    private final String path;
    private final List<String> names;
    private final boolean recursive;
    private final boolean withDecryption;

    private ScheduledExecutorService executorService;

    public SsmConfigSource(SsmClient client, String path, List<String> names, boolean recursive, boolean withDecryption,
            int updateIntervalMinutes, int ordinal) {
        super(CONFIG_SOURCE_NAME, ordinal);
        this.client = client;
        this.path = path;
        this.names = names != null ? List.copyOf(names) : List.of();
        this.recursive = recursive;
        this.withDecryption = withDecryption;

        reload();

        if (updateIntervalMinutes > 0) {
            LOG.debugf("SSM ConfigSource update interval set to %d minutes", updateIntervalMinutes);
            ThreadFactory threadFactory = runnable -> new Thread(runnable, POLL_THREAD_NAME);
            executorService = new ScheduledThreadPoolExecutor(1, threadFactory);
            executorService.scheduleAtFixedRate(this::reload, updateIntervalMinutes, updateIntervalMinutes,
                    TimeUnit.MINUTES);
        }
    }

    private void reload() {
        try {
            Map<String, String> merged = new LinkedHashMap<>();
            if (!isBlank(path)) {
                mergeParametersByPath(merged, path.trim());
            }
            for (List<String> batch : partition(names, GET_PARAMETERS_BATCH_SIZE)) {
                mergeParametersByName(merged, batch);
            }
            entriesRef.set(Collections.unmodifiableMap(merged));
            LOG.debugf("SSM ConfigSource now exposes %d configuration entries.", merged.size());
        } catch (Exception e) {
            LOG.errorf(e, "Failed to reload configuration from AWS Systems Manager Parameter Store.");
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

    private void mergeParametersByName(Map<String, String> target, List<String> batch) {
        if (batch.isEmpty()) {
            return;
        }
        var response = client.getParameters(GetParametersRequest.builder()
                .names(batch)
                .withDecryption(withDecryption)
                .build());
        for (Parameter p : response.parameters()) {
            mergeParameter(target, p.name(), p.value());
        }
        if (!response.invalidParameters().isEmpty()) {
            LOG.warnf("SSM GetParameters reported invalid parameter names: %s", response.invalidParameters());
        }
    }

    private static void mergeParameter(Map<String, String> target, String awsName, String value) {
        target.putAll(expandParameterValue(awsName, value));
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
        String prefix = normalizeParameterName(awsName);
        if (prefix.isEmpty()) {
            return Map.of();
        }
        if (value == null) {
            return Map.of();
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return Map.of();
        }

        // Use shared JsonConfigFlattener for JSON detection and flattening
        Map<String, String> flattened = JsonConfigFlattener.expandValue(prefix, trimmed);
        if (!flattened.isEmpty()) {
            return flattened;
        }

        // Handle properties format: if contains '=', parse as Java properties
        if (trimmed.contains("=")) {
            Properties p = new Properties();
            try {
                p.load(new StringReader(value));
            } catch (Exception e) {
                return Map.of(prefix, value);
            }
            if (!p.stringPropertyNames().isEmpty()) {
                Map<String, String> out = new LinkedHashMap<>();
                for (String n : p.stringPropertyNames()) {
                    out.put(prefix + "." + n, p.getProperty(n));
                }
                return out;
            }
        }
        return Map.of(prefix, value);
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
