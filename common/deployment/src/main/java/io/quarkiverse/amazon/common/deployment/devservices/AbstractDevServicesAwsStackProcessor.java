package io.quarkiverse.amazon.common.deployment.devservices;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkiverse.amazon.common.deployment.spi.AwsStackContainer;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackExtensionProvider;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.AwsStackDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.Startable;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.runtime.configuration.ConfigUtils;

/**
 * Abstract base processor for AWS stack dev services.
 * <p>
 * This processor is responsible for:
 * 1. Consuming all {@link DevServicesAwsStackProviderBuildItem} instances from service processors
 * 2. Managing a stack container lifecycle using the new Quarkus Dev Services API
 * 3. Publishing configuration for each requested service
 */
public abstract class AbstractDevServicesAwsStackProcessor {

    private static final String SERVICE_ENDPOINT_OVERRIDE = "quarkus.%s.endpoint-override";

    protected static final Logger log = Logger.getLogger(AbstractDevServicesAwsStackProcessor.class);

    /**
     * Returns the feature name for this stack (e.g., "amazon-ministack").
     */
    protected abstract String getFeatureName();

    /**
     * Returns the dev service label for this stack (e.g., "quarkus-dev-service-ministack").
     */
    protected abstract String getDevServiceLabel();

    /**
     * Returns the port used by this stack.
     */
    protected abstract int getStackPort();

    /**
     * Returns the container locator for this stack.
     */
    protected abstract ContainerLocator getContainerLocator();

    /**
     * Creates a Startable container for this stack.
     */
    protected abstract Startable createContainer(DevServicesComposeProjectBuildItem composeProjectBuildItem,
            AwsStackDevServicesBuildTimeConfig config, boolean useSharedNetwork,
            LaunchModeBuildItem launchMode, List<DevServicesAwsStackProviderBuildItem> requestedServices);

    /**
     * Creates an AwsStackContainer from an endpoint string.
     */
    protected abstract AwsStackContainer createAwsStackContainerFromEndpoint(String endpoint);

    public DevServicesResultBuildItem startAwsStackDevService(
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem compose,
            LaunchModeBuildItem launchMode,
            List<DevServicesSharedNetworkBuildItem> sharedNetwork,
            DevServicesConfig devServicesConfig,
            GlobalDevServicesBuildTimeConfig globalConfig,
            AwsStackDevServicesBuildTimeConfig config,
            List<DevServicesAwsStackProviderBuildItem> requestedServices) {

        if (requestedServices.isEmpty()) {
            log.debug("No extension requested dev services for Amazon Services, skipping the startup of dev services.");
            return null;
        }

        if (devServiceDisabled(dockerStatusBuildItem, config, requestedServices)) {
            return null;
        }
        boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig, sharedNetwork);

        // Try to locate an existing shared container
        ContainerLocator containerLocator = getContainerLocator();
        return containerLocator.locateContainer(config.serviceName(), config.shared(), launchMode.getLaunchMode())
                .or(() -> ComposeLocator.locateContainer(compose, List.of(config.imageName()), getStackPort(),
                        launchMode.getLaunchMode(), useSharedNetwork))
                .map(containerAddress -> {
                    // Found existing container - reuse it
                    reuseStackContainer(containerAddress.getUrl(), config, requestedServices);
                    Map<String, String> discoveredConfig = buildDiscoveredConfig(containerAddress.getUrl(), requestedServices);
                    return DevServicesResultBuildItem.discovered()
                            .feature(getFeatureName())
                            .containerId(containerAddress.getId())
                            .config(discoveredConfig)
                            .build();
                })
                .orElseGet(() -> {
                    return DevServicesResultBuildItem.owned()
                            .feature(getFeatureName())
                            .serviceName(config.serviceName())
                            .serviceConfig(buildServiceConfig(globalConfig, config, requestedServices))
                            .startable(() -> createContainer(compose, config, useSharedNetwork, launchMode, requestedServices))
                            .configProvider(buildConfigProvider(requestedServices))
                            .postStartHook(s -> logStartedAndPrepareStackContainer(s, requestedServices))
                            .build();
                });
    }

    private void logStartedAndPrepareStackContainer(Startable s,
            List<DevServicesAwsStackProviderBuildItem> requestedServices) {
        String prettyRequestServicesName = String.join(", ",
                requestedServices.stream().map(requestedService -> requestedService.getServiceName()).toArray(String[]::new));
        logStarted(getFeatureName(), prettyRequestServicesName, s.getConnectionInfo());
        AwsStackContainer awsStackContainer = createAwsStackContainerFromEndpoint(s.getConnectionInfo());
        requestedServices.forEach(requestedService -> requestedService.getDevProvider()
                .prepareAwsStackContainer(awsStackContainer));
    }

    private void reuseStackContainer(String endpointOverride, AwsStackDevServicesBuildTimeConfig config,
            List<DevServicesAwsStackProviderBuildItem> requestedServices) {
        AwsStackContainer awsStackContainer = createAwsStackContainerFromEndpoint(endpointOverride);
        requestedServices.forEach(requestedService -> requestedService.getDevProvider()
                .reuseAwsStackContainer(awsStackContainer));
    }

    private static void logStarted(String featureName, String prettyRequestServicesName, String endpointOverride) {
        log.infof(
                "Dev Services (%s) for Amazon Services started for services: %s. Other Quarkus applications in dev mode will find the "
                        + "stack automatically. You can connect to the stack at %s.",
                featureName, prettyRequestServicesName,
                endpointOverride);
    }

    private boolean devServiceDisabled(DockerStatusBuildItem dockerStatusBuildItem,
            AwsStackDevServicesBuildTimeConfig config,
            List<DevServicesAwsStackProviderBuildItem> requestedServices) {

        if (!config.enabled().orElse(true)) {
            // explicitly disabled
            log.debug("Not starting dev services for Amazon Services, as it has been disabled in the config.");
            return true;
        }

        // Check if any service has endpoint-override configured
        for (DevServicesAwsStackProviderBuildItem service : requestedServices) {
            String endpointOverride = String.format(SERVICE_ENDPOINT_OVERRIDE, service.getServiceName());
            if (ConfigUtils.isPropertyPresent(endpointOverride)) {
                log.debugf("Not starting dev services for Amazon Services - %s, endpoint-override is configured.",
                        service.getServiceName());
                return true;
            }
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            log.warn(
                    "Docker isn't working, dev services for Amazon Services is not available. Please configure the endpoint-override properties.");
            return true;
        }

        return false;
    }

    private Map<String, String> buildDiscoveredConfig(String endpoint,
            List<DevServicesAwsStackProviderBuildItem> requestedServices) {
        Map<String, String> config = new HashMap<>();

        AwsStackContainer container = createAwsStackContainerFromEndpoint(endpoint);

        for (DevServicesAwsStackProviderBuildItem service : requestedServices) {
            var applicationConfigProvider = service.getDevProvider().getClientConfig();

            applicationConfigProvider.forEach((key, valueFunction) -> {
                config.put(key, valueFunction.apply(container));
            });
        }

        return config;
    }

    private Map<String, Function<Startable, String>> buildConfigProvider(
            List<DevServicesAwsStackProviderBuildItem> requestedServices) {
        Map<String, Function<Startable, String>> configProvider = new HashMap<>();

        for (DevServicesAwsStackProviderBuildItem requestedService : requestedServices) {
            DevServicesAwsStackExtensionProvider provider = requestedService.getDevProvider();

            var sampleConfig = provider.getClientConfig();

            sampleConfig.forEach((key, valueFunction) -> {
                configProvider.put(key, startable -> {
                    AwsStackContainer awsStackContainer = createAwsStackContainerFromEndpoint(startable.getConnectionInfo());
                    return valueFunction.apply(awsStackContainer);
                });
            });
        }

        return configProvider;
    }

    private ServiceConfig buildServiceConfig(
            GlobalDevServicesBuildTimeConfig globalConfig,
            AwsStackDevServicesBuildTimeConfig config,
            List<DevServicesAwsStackProviderBuildItem> requestedServices) {
        return new ServiceConfig() {
            @Override
            public GlobalDevServicesBuildTimeConfig getGlobalConfig() {
                return globalConfig;
            }

            @Override
            public AwsStackDevServicesBuildTimeConfig getStackConfig() {
                return config;
            }

            @Override
            public Set<DevServicesBuildTimeConfig> getServiceConfigs() {
                return requestedServices.stream()
                        .map(DevServicesAwsStackProviderBuildItem::getConfig)
                        .collect(Collectors.toSet());
            }
        };
    }

    public interface ServiceConfig {
        GlobalDevServicesBuildTimeConfig getGlobalConfig();

        AwsStackDevServicesBuildTimeConfig getStackConfig();

        Set<DevServicesBuildTimeConfig> getServiceConfigs();
    }
}
