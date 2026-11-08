package io.quarkiverse.amazon.common.config;

/** Only configured entry names are safe to include in this diagnostic. */
public final class ConfigSourceCollisionException extends IllegalArgumentException {
    ConfigSourceCollisionException(String first, String second) {
        super("Configuration entries collide: " + first + " and " + second);
    }
}
