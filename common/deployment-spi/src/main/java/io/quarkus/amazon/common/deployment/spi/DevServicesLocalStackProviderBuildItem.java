package io.quarkus.amazon.common.deployment.spi;

import org.testcontainers.containers.localstack.LocalStackContainer.EnabledService;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * BuildItem to request a localstack dev service container.
 *
 */
public final class DevServicesLocalStackProviderBuildItem extends MultiBuildItem {
    private final EnabledService service;
    private final DevServicesAmazonProvider devProvider;
    private final LocalStackDevServicesBaseConfig config;

    /**
     * ctor
     *
     * @param enabledService amamzon service type the container must expose
     * @param config configuration for this service
     * @param devProvider provider to configure the container and get client configuration properties
     */
    public DevServicesLocalStackProviderBuildItem(EnabledService enabledService,
            LocalStackDevServicesBaseConfig config,
            DevServicesAmazonProvider devProvider) {
        this.service = enabledService;
        this.config = config;
        this.devProvider = devProvider;
    }

    public EnabledService getService() {
        return service;
    }

    public DevServicesAmazonProvider getDevProvider() {
        return devProvider;
    }

    public LocalStackDevServicesBaseConfig getConfig() {
        return config;
    }
}
