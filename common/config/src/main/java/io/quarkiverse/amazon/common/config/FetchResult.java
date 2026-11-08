package io.quarkiverse.amazon.common.config;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** A provider retains protocol state even when publication of this result fails. */
public record FetchResult(Map<ResourceId, RawDocument> updated, Set<ResourceId> unchanged,
        Set<ResourceId> removed, Map<ResourceId, FetchFailure> failed, FetchFailure batchFailure,
        Duration minDelayBeforeNextFetch) {
    public FetchResult {
        updated = Map.copyOf(updated);
        unchanged = Set.copyOf(unchanged);
        removed = Set.copyOf(removed);
        failed = Map.copyOf(failed);
        Objects.requireNonNull(minDelayBeforeNextFetch);
        if (minDelayBeforeNextFetch.isNegative()) {
            throw new IllegalArgumentException("A polling delay cannot be negative.");
        }
        Set<ResourceId> seen = new HashSet<>();
        for (Set<ResourceId> group : java.util.List.of(updated.keySet(), unchanged, removed, failed.keySet())) {
            for (ResourceId id : group) {
                if (!seen.add(id)) {
                    throw new IllegalArgumentException("A resource has conflicting fetch states.");
                }
            }
        }
        updated.forEach((id, document) -> {
            if (!id.equals(document.id())) {
                throw new IllegalArgumentException("A fetched document has a different resource identity.");
            }
        });
    }
}
