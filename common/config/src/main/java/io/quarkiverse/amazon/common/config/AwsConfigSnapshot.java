package io.quarkiverse.amazon.common.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/** Transport progress is durable across failed publications; readers only see complete immutable snapshots. */
public final class AwsConfigSnapshot {
    private final Map<ResourceId, RawDocument> working = new LinkedHashMap<>();
    private final Set<ResourceId> known = new HashSet<>();
    private final AtomicReference<Map<String, String>> published = new AtomicReference<>(Map.of());

    public synchronized boolean apply(FetchResult result, Function<ResourceId, List<ConfigSourceEntry>> entries) {
        working.putAll(result.updated());
        known.addAll(result.updated().keySet());
        known.addAll(result.failed().keySet());
        result.removed().forEach(working::remove);
        known.removeAll(result.removed());
        if (result.batchFailure() != null || !result.failed().isEmpty()) {
            return false;
        }
        var accounted = new HashSet<>(result.updated().keySet());
        accounted.addAll(result.unchanged());
        if (!accounted.containsAll(known) || !working.keySet().containsAll(result.unchanged())) {
            throw new IllegalArgumentException("The provider omitted a resource or reported an unknown unchanged resource.");
        }
        Map<String, String> merged = new LinkedHashMap<>();
        Map<String, String> owners = new HashMap<>();
        for (RawDocument raw : working.values()) {
            List<ConfigSourceEntry> consumers = entries.apply(raw.id());
            if (consumers.isEmpty()) {
                throw new IllegalArgumentException("A fetched resource has no configured entry.");
            }
            for (ConfigSourceEntry entry : consumers) {
                for (var property : entry.decode(raw).entrySet()) {
                    String previous = owners.putIfAbsent(property.getKey(), entry.name());
                    if (previous != null) {
                        throw new ConfigSourceCollisionException(previous, entry.name());
                    }
                    merged.put(property.getKey(), property.getValue());
                }
            }
        }
        published.set(Map.copyOf(merged));
        return true;
    }

    public Map<String, String> properties() {
        return published.get();
    }
}
