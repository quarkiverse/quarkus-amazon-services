package io.quarkiverse.amazon.common.config;

/** Safe failure categories; no SDK exceptions, payloads or tokens cross the provider boundary. */
public enum FetchFailure {
    MISSING,
    ACCESS_DENIED,
    TRANSIENT,
    INVALID_CONTENT,
    PARTIAL_RESPONSE,
    OTHER
}
