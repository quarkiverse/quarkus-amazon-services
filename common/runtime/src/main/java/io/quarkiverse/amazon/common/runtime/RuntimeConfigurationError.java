package io.quarkiverse.amazon.common.runtime;

public class RuntimeConfigurationError extends RuntimeException {

    public RuntimeConfigurationError(String message) {
        super(message);
    }
}
