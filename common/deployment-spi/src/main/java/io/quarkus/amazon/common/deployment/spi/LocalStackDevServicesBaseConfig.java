package io.quarkus.amazon.common.deployment.spi;

import java.util.Map;
import java.util.Objects;

public class LocalStackDevServicesBaseConfig {
    private final boolean shared;
    private final String serviceName;
    private final Map<String, String> containerProperties;

    public LocalStackDevServicesBaseConfig(boolean shared, String serviceName,
            Map<String, String> containerProperties) {
        this.shared = shared;
        this.serviceName = serviceName;
        this.containerProperties = containerProperties;
    }

    public boolean isShared() {
        return shared;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Map<String, String> getContainerProperties() {
        return containerProperties;
    }

    @Override
    public int hashCode() {
        return Objects.hash(shared, serviceName, containerProperties);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LocalStackDevServicesBaseConfig that = (LocalStackDevServicesBaseConfig) o;
        return shared == that.shared
                && Objects.equals(serviceName, that.serviceName)
                && Objects.equals(containerProperties, that.containerProperties);
    }
}
