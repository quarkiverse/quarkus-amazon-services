package io.quarkus.amazon.common.runtime;

public final class ClientUtil {
    public static final String DEFAULT_CLIENT_NAME = "<default>";

    public static boolean isDefaultClient(String clientName) {
        return DEFAULT_CLIENT_NAME.equalsIgnoreCase(clientName);
    }
}
