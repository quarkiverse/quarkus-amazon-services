package io.quarkus.amazon.common.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Produced when a client has telemetry enabled
 */
public final class RequireAmazonTelemetryBuildItem extends MultiBuildItem {

    private String configName;

    public RequireAmazonTelemetryBuildItem(String configName) {
        this.configName = configName;
    }

    public String getConfigName() {
        return configName;
    }
}
