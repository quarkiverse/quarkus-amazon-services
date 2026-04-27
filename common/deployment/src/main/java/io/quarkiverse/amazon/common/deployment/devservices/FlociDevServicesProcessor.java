package io.quarkiverse.amazon.common.deployment.devservices;

import static io.quarkus.devservices.common.ConfigureUtil.configureSharedServiceLabel;
import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import io.floci.testcontainers.FlociContainer;
import io.quarkiverse.amazon.common.deployment.spi.AwsStackContainer;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.AwsStackDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.FlociDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
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
 * Processor for Floci dev services.
 */
@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class })
public class FlociDevServicesProcessor extends AbstractDevServicesAwsStackProcessor {

    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-floci";
    private static final String FEATURE_NAME = "amazon-floci";
    private static final int FLOCI_PORT = 4566;

    private static final ContainerLocator flociContainerLocator = locateContainerWithLabels(FLOCI_PORT,
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
        return FLOCI_PORT;
    }

    @Override
    protected ContainerLocator getContainerLocator() {
        return flociContainerLocator;
    }

    @BuildStep
    public DevServicesResultBuildItem startFlociDevService(
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem compose,
            LaunchModeBuildItem launchMode,
            List<DevServicesSharedNetworkBuildItem> sharedNetwork,
            DevServicesConfig devServicesConfig,
            GlobalDevServicesBuildTimeConfig globalConfig,
            FlociDevServicesBuildTimeConfig config,
            List<DevServicesAwsStackProviderBuildItem> requestedServices) {

        var requestedFlociServices = requestedServices
                .stream()
                .filter(rs -> rs.getConfig().provider().orElse(globalConfig.provider())
                        .equals(GlobalDevServicesBuildTimeConfig.AwsStack.FLOCI))
                .collect(Collectors.toList());

        return startAwsStackDevService(dockerStatusBuildItem, compose, launchMode, sharedNetwork, devServicesConfig,
                globalConfig, config,
                requestedFlociServices);
    }

    @Override
    protected Startable createContainer(DevServicesComposeProjectBuildItem composeProjectBuildItem,
            AwsStackDevServicesBuildTimeConfig config, boolean useSharedNetwork,
            LaunchModeBuildItem launchMode, List<DevServicesAwsStackProviderBuildItem> requestedServices) {
        FlociContainer floci = new FlociContainer(DockerImageName.parse(config.imageName()));

        config.initScriptsFolder().ifPresentOrElse(initScriptsFolder -> {
            floci.withFileSystemBind(initScriptsFolder, "/etc/floci/init/start.d", BindMode.READ_ONLY);
        }, () -> config.initScriptsClasspath().ifPresent(resourcePath -> {
            // scripts must be executable but withClasspathResourceMapping will extract file with read only for regular file
            final MountableFile mountableFile = MountableFile.forClasspathResource(resourcePath, 555);
            floci.withCopyFileToContainer(mountableFile, "/etc/floci/init/start.d");
        }));

        config.initCompletionMsg().ifPresent(initCompletionMsg -> {
            floci.waitingFor(Wait.forLogMessage(".*" + initCompletionMsg + ".*\\n", 1));
        });

        ConfigureUtil.configureNetwork(floci, composeProjectBuildItem.getDefaultNetworkId(), useSharedNetwork, "floci");

        floci.withEnv(config.containerProperties());
        requestedServices.stream().map(r -> r.getConfig().containerProperties()).forEach(floci::withEnv);

        configureSharedServiceLabel(floci, launchMode.getLaunchMode(), DEV_SERVICE_LABEL, config.serviceName());

        config.port().ifPresent(
                port -> floci.setPortBindings(Collections.singletonList("%s:%s".formatted(port, FLOCI_PORT))));

        return new StartableContainer<>(floci, FlociContainer::getEndpoint);
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
                return "floci";
            }

            @Override
            public String getSecretKey() {
                return "floci";
            }
        };
    }
}