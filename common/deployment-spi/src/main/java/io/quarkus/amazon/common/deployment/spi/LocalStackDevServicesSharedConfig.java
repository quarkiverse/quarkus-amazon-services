package io.quarkus.amazon.common.deployment.spi;

public class LocalStackDevServicesSharedConfig {
    private final boolean shared;
    private final String serviceName;

    public LocalStackDevServicesSharedConfig(boolean shared, String serviceName) {
        this.shared = shared;
        this.serviceName = serviceName;
    }

    public boolean isShared() {
        return shared;
    }

    public String getServiceName() {
        return serviceName;
    }
}
