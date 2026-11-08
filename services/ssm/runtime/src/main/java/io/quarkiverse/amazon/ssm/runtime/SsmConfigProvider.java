package io.quarkiverse.amazon.ssm.runtime;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import io.quarkiverse.amazon.common.config.AwsConfigProvider;
import io.quarkiverse.amazon.common.config.FetchFailure;
import io.quarkiverse.amazon.common.config.FetchResult;
import io.quarkiverse.amazon.common.config.RawDocument;
import io.quarkiverse.amazon.common.config.ResourceId;
import io.quarkiverse.amazon.common.runtime.ConfigSourceFailure;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersRequest;
import software.amazon.awssdk.services.ssm.model.Parameter;

/** SSM pagination is complete only after every selected path/page and name batch succeeds. */
public final class SsmConfigProvider implements AwsConfigProvider {
    public record Path(String value, boolean recursive) {
        public Path {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("SSM configuration requires a non-empty path.");
            }
            value = value.trim();
            if (!value.startsWith("/")) {
                value = "/" + value;
            }
            while (value.length() > 1 && value.endsWith("/")) {
                value = value.substring(0, value.length() - 1);
            }
        }
    }

    private final SsmClient client;
    private final List<Path> paths;
    private final List<String> names;
    private final boolean withDecryption;
    private final Set<ResourceId> known = new HashSet<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    public SsmConfigProvider(SsmClient client, String path, List<String> names, boolean recursive, boolean withDecryption) {
        this(client, path == null || path.isBlank() ? List.of() : List.of(new Path(path, recursive)), names, withDecryption);
    }

    public SsmConfigProvider(SsmClient client, List<Path> paths, List<String> names, boolean withDecryption) {
        this.client = client;
        this.paths = List.copyOf(new LinkedHashSet<>(paths));
        this.names = names == null ? List.of() : new ArrayList<>(new LinkedHashSet<>(names));
        this.withDecryption = withDecryption;
    }

    @Override
    public FetchResult fetchAll() {
        ensureOpen();
        Map<ResourceId, RawDocument> updated = new LinkedHashMap<>();
        Map<ResourceId, FetchFailure> failed = new LinkedHashMap<>();
        Set<ResourceId> selected = new HashSet<>();
        FetchFailure batchFailure = null;
        for (Path path : paths) {
            ensureOpen();
            try {
                String nextToken = null;
                do {
                    ensureOpen();
                    var response = client.getParametersByPath(GetParametersByPathRequest.builder()
                            .path(path.value()).recursive(path.recursive())
                            .withDecryption(withDecryption).nextToken(nextToken).build());
                    for (Parameter parameter : response.parameters()) {
                        add(parameter, updated, failed, selected);
                    }
                    nextToken = response.nextToken();
                } while (nextToken != null && !nextToken.isEmpty());
            } catch (Exception e) {
                batchFailure = classify(e);
            }
        }
        for (int from = 0; from < names.size(); from += 10) {
            ensureOpen();
            List<String> batch = names.subList(from, Math.min(from + 10, names.size()));
            try {
                var response = client
                        .getParameters(GetParametersRequest.builder().names(batch).withDecryption(withDecryption).build());
                Set<String> accounted = new HashSet<>(response.invalidParameters());
                for (String missing : response.invalidParameters()) {
                    ResourceId id = new ResourceId(missing);
                    selected.add(id);
                    updated.remove(id);
                    failed.put(id, FetchFailure.MISSING);
                }
                for (Parameter parameter : response.parameters()) {
                    add(parameter, updated, failed, selected);
                    String selector = parameter.selector() == null ? "" : parameter.selector();
                    for (String requested : batch) {
                        if (requested.equals(parameter.name() + selector)
                                || parameter.arn() != null && requested.equals(parameter.arn() + selector)) {
                            accounted.add(requested);
                        }
                    }
                }
                if (!accounted.containsAll(batch)) {
                    batchFailure = FetchFailure.PARTIAL_RESPONSE;
                }
            } catch (Exception e) {
                batchFailure = classify(e);
            }
        }
        Set<ResourceId> removed = new HashSet<>();
        if (batchFailure == null) {
            removed.addAll(known);
            removed.removeAll(selected);
            known.removeAll(removed);
        }
        known.addAll(selected);
        return new FetchResult(updated, Set.of(), removed, failed, batchFailure, Duration.ZERO);
    }

    private void add(Parameter parameter, Map<ResourceId, RawDocument> updated, Map<ResourceId, FetchFailure> failed,
            Set<ResourceId> selected) {
        String selector = parameter.selector() == null ? "" : parameter.selector();
        ResourceId id = new ResourceId(parameter.name() + selector);
        selected.add(id);
        try {
            updated.put(id, RawDocument.text(id, parameter.value(), null,
                    parameter.version() == null ? null : Long.toString(parameter.version())));
            failed.remove(id);
        } catch (Exception e) {
            updated.remove(id);
            failed.put(id, classify(e));
        }
    }

    private FetchFailure classify(Exception e) {
        return ConfigSourceFailure.classify(e);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            client.close();
        }
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("SSM provider is closed.");
        }
    }
}
