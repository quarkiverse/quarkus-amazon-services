package io.quarkus.amazon.common.deployment.spi;

import java.util.Map;
import java.util.Objects;

public class LocalStackDevServicesBaseConfig {
    private final boolean shared;
    private final boolean isolated;
    private final String serviceName;
    private final Map<String, String> containerProperties;

    public LocalStackDevServicesBaseConfig(boolean shared, boolean isolated, String serviceName,
            Map<String, String> containerProperties) {
        this.shared = shared;
        this.isolated = isolated;
        this.serviceName = serviceName;
        this.containerProperties = containerProperties;
    }

    public boolean isShared() {
        return shared;
    }

    public boolean isIsolated() {
        return isolated;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Map<String, String> getContainerProperties() {
        return containerProperties;
    }

    @Override
    public int hashCode() {
        return Objects.hash(shared, isolated, serviceName, containerProperties);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LocalStackDevServicesBaseConfig that = (LocalStackDevServicesBaseConfig) o;
        return shared == that.shared
                && isolated == that.isolated
                && Objects.equals(serviceName, that.serviceName)
                && Objects.equals(containerProperties, that.containerProperties);
    }
}
