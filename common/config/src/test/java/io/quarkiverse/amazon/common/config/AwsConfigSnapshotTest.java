package io.quarkiverse.amazon.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkiverse.amazon.common.runtime.ConfigSourceFormat;

class AwsConfigSnapshotTest {
    private final ResourceId a = new ResourceId("a");
    private final ResourceId b = new ResourceId("b");
    private final AwsConfigSnapshot snapshot = new AwsConfigSnapshot();

    @Test
    void changedResourceIsRetainedWhenAnotherFailsThenReturnsUnchanged() {
        apply(Map.of(a, raw(a, "old-a"), b, raw(b, "old-b")), Set.of(), Set.of(), Map.of(), null);
        assertFalse(apply(Map.of(a, raw(a, "new-a")), Set.of(), Set.of(), Map.of(b, FetchFailure.TRANSIENT), null));
        assertEquals(Map.of("a", "old-a", "b", "old-b"), snapshot.properties());
        assertTrue(apply(Map.of(b, raw(b, "new-b")), Set.of(a), Set.of(), Map.of(), null));
        assertEquals(Map.of("a", "new-a", "b", "new-b"), snapshot.properties());
    }

    @Test
    void incompleteWalkNeverInfersDeletion() {
        apply(Map.of(a, raw(a, "old-a"), b, raw(b, "old-b")), Set.of(), Set.of(), Map.of(), null);
        assertFalse(apply(Map.of(a, raw(a, "new-a")), Set.of(), Set.of(), Map.of(), FetchFailure.TRANSIENT));
        assertEquals(Map.of("a", "old-a", "b", "old-b"), snapshot.properties());
        apply(Map.of(b, raw(b, "new-b")), Set.of(a), Set.of(), Map.of(), null);
        assertEquals(Map.of("a", "new-a", "b", "new-b"), snapshot.properties());
    }

    @Test
    void confirmedDeletionSurvivesAFailedCycle() {
        apply(Map.of(a, raw(a, "old-a"), b, raw(b, "old-b")), Set.of(), Set.of(), Map.of(), null);
        assertFalse(apply(Map.of(), Set.of(), Set.of(a), Map.of(b, FetchFailure.TRANSIENT), null));
        assertEquals(Map.of("a", "old-a", "b", "old-b"), snapshot.properties());
        apply(Map.of(), Set.of(b), Set.of(), Map.of(), null);
        assertEquals(Map.of("b", "old-b"), snapshot.properties());
    }

    @Test
    void invalidContentAndUnknownUnchangedResourcesDoNotReplaceSnapshot() {
        apply(Map.of(a, raw(a, "old")), Set.of(), Set.of(), Map.of(), null);
        RawDocument invalid = new RawDocument(a, new byte[] { (byte) 0xff, (byte) 0xfe }, null, null);
        assertThrows(IllegalArgumentException.class, () -> apply(Map.of(a, invalid), Set.of(), Set.of(), Map.of(), null));
        assertEquals(Map.of("a", "old"), snapshot.properties());
        assertThrows(IllegalArgumentException.class,
                () -> apply(Map.of(a, raw(a, "new")), Set.of(b), Set.of(), Map.of(), null));
        assertEquals(Map.of("a", "old"), snapshot.properties());
    }

    @Test
    void oneResourceCanHaveTwoIndependentEntries() {
        var result = result(Map.of(a, raw(a, "{\"password\":\"value\"}")), Set.of(), Set.of(), Map.of(), null);
        snapshot.apply(result, id -> List.of(new ConfigSourceEntry("first", id, "first", ConfigSourceFormat.JSON, false),
                new ConfigSourceEntry("second", id, "second", ConfigSourceFormat.JSON, false)));
        assertEquals(Map.of("first.password", "value", "second.password", "value"), snapshot.properties());
    }

    @Test
    void collisionsAreRejectedEvenWhenValuesMatch() {
        var result = result(Map.of(a, raw(a, "{\"password\":\"value\"}")), Set.of(), Set.of(), Map.of(), null);
        var error = assertThrows(IllegalArgumentException.class, () -> snapshot.apply(result,
                id -> List.of(new ConfigSourceEntry("first", id, "db", ConfigSourceFormat.JSON, false),
                        new ConfigSourceEntry("second", id, "db", ConfigSourceFormat.JSON, false))));
        assertEquals("Configuration entries collide: first and second", error.getMessage());
        assertEquals(Map.of(), snapshot.properties());
    }

    @Test
    void documentsAndSnapshotsAreImmutable() {
        byte[] bytes = "old".getBytes(StandardCharsets.UTF_8);
        var raw = new RawDocument(a, bytes, null, null);
        bytes[0] = 'x';
        raw.content()[0] = 'x';
        apply(Map.of(a, raw), Set.of(), Set.of(), Map.of(), null);
        assertEquals(Map.of("a", "old"), snapshot.properties());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.properties().put("a", "bad"));
    }

    @Test
    void conflictingStatesAndMismatchedDocumentsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> result(Map.of(a, raw(a, "value")), Set.of(a), Set.of(), Map.of(), null));
        assertThrows(IllegalArgumentException.class,
                () -> result(Map.of(a, raw(b, "value")), Set.of(), Set.of(), Map.of(), null));
    }

    @Test
    void previouslyFailedResourcesCannotBeSilentlyOmitted() {
        assertFalse(apply(Map.of(a, raw(a, "value-a")), Set.of(), Set.of(), Map.of(b, FetchFailure.TRANSIENT), null));
        assertThrows(IllegalArgumentException.class,
                () -> apply(Map.of(), Set.of(a), Set.of(), Map.of(), null));
        assertEquals(Map.of(), snapshot.properties());
        assertTrue(apply(Map.of(b, raw(b, "value-b")), Set.of(a), Set.of(), Map.of(), null));
        assertEquals(Map.of("a", "value-a", "b", "value-b"), snapshot.properties());
    }

    @Test
    void omittedExistingResourcesAreRejectedAndOlderMapsRemainStableAfterRecovery() {
        apply(Map.of(a, raw(a, "old-a"), b, raw(b, "old-b")), Set.of(), Set.of(), Map.of(), null);
        Map<String, String> previous = snapshot.properties();
        assertThrows(IllegalArgumentException.class,
                () -> apply(Map.of(a, raw(a, "new-a")), Set.of(), Set.of(), Map.of(), null));
        assertEquals(previous, snapshot.properties());
        apply(Map.of(b, raw(b, "new-b")), Set.of(a), Set.of(), Map.of(), null);
        assertEquals(Map.of("a", "new-a", "b", "new-b"), snapshot.properties());
        assertEquals(Map.of("a", "old-a", "b", "old-b"), previous);
    }

    @Test
    void invalidDocumentCanBeReplacedByAValidUpdate() {
        apply(Map.of(a, raw(a, "old")), Set.of(), Set.of(), Map.of(), null);
        var invalid = new RawDocument(a, new byte[] { (byte) 0xff }, null, null);
        assertThrows(IllegalArgumentException.class, () -> apply(Map.of(a, invalid), Set.of(), Set.of(), Map.of(), null));
        apply(Map.of(a, raw(a, "new")), Set.of(), Set.of(), Map.of(), null);
        assertEquals(Map.of("a", "new"), snapshot.properties());
    }

    private boolean apply(Map<ResourceId, RawDocument> updated, Set<ResourceId> unchanged, Set<ResourceId> removed,
            Map<ResourceId, FetchFailure> failed, FetchFailure batchFailure) {
        return snapshot.apply(result(updated, unchanged, removed, failed, batchFailure),
                id -> List.of(new ConfigSourceEntry(id.value(), id, id.value(), ConfigSourceFormat.RAW, false)));
    }

    private FetchResult result(Map<ResourceId, RawDocument> updated, Set<ResourceId> unchanged, Set<ResourceId> removed,
            Map<ResourceId, FetchFailure> failed, FetchFailure batchFailure) {
        return new FetchResult(updated, unchanged, removed, failed, batchFailure, Duration.ZERO);
    }

    private RawDocument raw(ResourceId id, String value) {
        return new RawDocument(id, value.getBytes(StandardCharsets.UTF_8), null, null);
    }
}
