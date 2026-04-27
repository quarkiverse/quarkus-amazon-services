package io.quarkiverse.amazon.common.deployment.devservices;

import static io.quarkus.devservices.common.ConfigureUtil.configureSharedServiceLabel;
import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.testcontainers.utility.DockerImageName;

import io.quarkiverse.amazon.common.deployment.spi.AwsStackContainer;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.AwsStackDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.MotoDevServicesBuildTimeConfig;
import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.Startable;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.devservices.common.StartableContainer;

/**
 * Processor for Moto dev services.
 */
@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class })
public class MotoDevServicesProcessor extends AbstractDevServicesAwsStackProcessor {

    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-moto";
    private static final String FEATURE_NAME = "amazon-moto";
    private static final int MOTO_PORT = 5000;

    private static final ContainerLocator motoContainerLocator = locateContainerWithLabels(MOTO_PORT,
            DEV_SERVICE_LABEL);

    @Override
    protected String getFeatureName() {
        return FEATURE_NAME;
    }

    @Override
    protected String getDevServiceLabel() {
        return DEV_SERVICE_LABEL;
    }

    @Override
    protected int getStackPort() {
        return MOTO_PORT;
    }

    @Override
    protected ContainerLocator getContainerLocator() {
        return motoContainerLocator;
    }

    @BuildStep
    public DevServicesResultBuildItem startMotoDevService(
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem compose,
            LaunchModeBuildItem launchMode,
            List<DevServicesSharedNetworkBuildItem> sharedNetwork,
            DevServicesConfig devServicesConfig,
            GlobalDevServicesBuildTimeConfig globalConfig,
            MotoDevServicesBuildTimeConfig config,
            List<DevServicesAwsStackProviderBuildItem> requestedServices) {

        var requestedMotoServices = requestedServices
                .stream()
                .filter(rs -> rs.getConfig().provider().orElse(globalConfig.provider())
                        .equals(GlobalDevServicesBuildTimeConfig.AwsStack.MOTO))
                .collect(Collectors.toList());

        return startAwsStackDevService(dockerStatusBuildItem, compose, launchMode, sharedNetwork, devServicesConfig,
                globalConfig, config,
                requestedMotoServices);
    }

    @Override
    protected Startable createContainer(DevServicesComposeProjectBuildItem composeProjectBuildItem,
            AwsStackDevServicesBuildTimeConfig config, boolean useSharedNetwork,
            LaunchModeBuildItem launchMode, List<DevServicesAwsStackProviderBuildItem> requestedServices) {
        MotoContainer moto = new MotoContainer(DockerImageName.parse(config.imageName()));

        ConfigureUtil.configureNetwork(moto, composeProjectBuildItem.getDefaultNetworkId(), useSharedNetwork, "moto");

        moto.withEnv(config.containerProperties());
        requestedServices.stream().map(r -> r.getConfig().containerProperties()).forEach(moto::withEnv);

        configureSharedServiceLabel(moto, launchMode.getLaunchMode(), DEV_SERVICE_LABEL, config.serviceName());

        config.port().ifPresent(
                port -> moto.setPortBindings(Collections.singletonList("%s:%s".formatted(port, MOTO_PORT))));

        return new StartableContainer<>(moto, MotoContainer::getEndpoint);
    }

    @Override
    protected AwsStackContainer createAwsStackContainerFromEndpoint(String endpoint) {
        return new AwsStackContainer() {
            @Override
            public URI getEndpoint() {
                return URI.create(endpoint);
            }

            @Override
            public String getRegion() {
                return "us-east-1";
            }

            @Override
            public String getAccessKey() {
                return "testing";
            }

            @Override
            public String getSecretKey() {
                return "testing";
            }
        };
    }
}
