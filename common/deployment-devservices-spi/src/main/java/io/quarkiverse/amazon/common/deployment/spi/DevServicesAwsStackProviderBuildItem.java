package io.quarkiverse.amazon.common.deployment.spi;

import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkus.builder.item.MultiBuildItem;

/**
 * BuildItem to request a MiniStack dev service container.
 * <p>
 * Service processors emit this BuildItem when MiniStack stack is selected.
 * A central DevServicesMiniStackProcessor consumes all instances of this BuildItem
 * and manages the MiniStack containers using the new Quarkus Dev Services API.
 * </p>
 */
public final class DevServicesAwsStackProviderBuildItem extends MultiBuildItem {
    private final String serviceName;
    private final DevServicesAwsStackExtensionProvider devProvider;
    private final DevServicesBuildTimeConfig config;

    /**
     * Constructor
     *
     * @param serviceName the AWS service name (e.g., "s3", "sqs")
     * @param config configuration for this service
     * @param devProvider provider to configure the container and get client configuration properties
     */
    public DevServicesAwsStackProviderBuildItem(String serviceName,
            DevServicesBuildTimeConfig config,
            DevServicesAwsStackExtensionProvider devProvider) {
        this.serviceName = serviceName;
        this.config = config;
        this.devProvider = devProvider;
    }

    public String getServiceName() {
        return serviceName;
    }

    public DevServicesAwsStackExtensionProvider getDevProvider() {
        return devProvider;
    }

    public DevServicesBuildTimeConfig getConfig() {
        return config;
    }
}
