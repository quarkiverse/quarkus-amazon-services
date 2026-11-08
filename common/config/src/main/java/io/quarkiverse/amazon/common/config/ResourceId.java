package io.quarkiverse.amazon.common.config;

/** Remote identity, independent of the configured entries that consume it. */
public record ResourceId(String value) {
    public ResourceId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A remote resource requires a non-empty identity.");
        }
    }
}
