package io.quarkus.amazon.common.deployment.spi;

import java.util.Map;

import org.testcontainers.containers.localstack.LocalStackContainer.EnabledService;

import io.quarkus.builder.item.MultiBuildItem;

public final class DevServicesLocalStackProviderBuildItem extends MultiBuildItem {
    private final EnabledService service;
    private final Map<String, String> env;

    private final DevServicesAmazonProvider devProvider;
    private final LocalStackDevServicesSharedConfig sharedConfig;

    public DevServicesLocalStackProviderBuildItem(EnabledService service,
            Map<String, String> env,
            LocalStackDevServicesSharedConfig sharedConfig,
            DevServicesAmazonProvider devProvider) {
        this.service = service;
        this.env = env;
        this.sharedConfig = sharedConfig;
        this.devProvider = devProvider;
    }

    public EnabledService getService() {
        return service;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public DevServicesAmazonProvider getDevProvider() {
        return devProvider;
    }

    public LocalStackDevServicesSharedConfig getSharedConfig() {
        return sharedConfig;
    }
}
