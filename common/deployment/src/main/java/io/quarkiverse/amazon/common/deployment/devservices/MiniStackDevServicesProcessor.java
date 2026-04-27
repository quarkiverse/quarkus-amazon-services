package io.quarkiverse.amazon.common.deployment.devservices;

import static io.quarkus.devservices.common.ConfigureUtil.configureSharedServiceLabel;
import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.ministack.testcontainers.MiniStackContainer;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import io.quarkiverse.amazon.common.deployment.spi.AwsStackContainer;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.AwsStackDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.MiniStackDevServicesBuildTimeConfig;
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
 * Processor for Ministack dev services.
 */
@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class })
public class MiniStackDevServicesProcessor extends AbstractDevServicesAwsStackProcessor {

    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-ministack";
    private static final String FEATURE_NAME = "amazon-ministack";
    private static final int MINISTACK_PORT = 4566;

    private static final ContainerLocator miniStackContainerLocator = locateContainerWithLabels(MINISTACK_PORT,
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
        return MINISTACK_PORT;
    }

    @Override
    protected ContainerLocator getContainerLocator() {
        return miniStackContainerLocator;
    }

    @BuildStep
    public DevServicesResultBuildItem startMiniStackDevService(
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem compose,
            LaunchModeBuildItem launchMode,
            List<DevServicesSharedNetworkBuildItem> sharedNetwork,
            DevServicesConfig devServicesConfig,
            GlobalDevServicesBuildTimeConfig globalConfig,
            MiniStackDevServicesBuildTimeConfig config,
            List<DevServicesAwsStackProviderBuildItem> requestedServices) {

        var requestedMiniStackServices = requestedServices
                .stream()
                .filter(rs -> rs.getConfig().provider().orElse(globalConfig.provider())
                        .equals(GlobalDevServicesBuildTimeConfig.AwsStack.MINISTACK))
                .collect(Collectors.toList());

        return startAwsStackDevService(dockerStatusBuildItem, compose, launchMode, sharedNetwork, devServicesConfig,
                globalConfig, config,
                requestedMiniStackServices);
    }

    @Override
    protected Startable createContainer(DevServicesComposeProjectBuildItem composeProjectBuildItem,
            AwsStackDevServicesBuildTimeConfig config, boolean useSharedNetwork,
            LaunchModeBuildItem launchMode, List<DevServicesAwsStackProviderBuildItem> requestedServices) {
        MiniStackContainer miniStack = new MiniStackContainer(DockerImageName.parse(config.imageName()));

        config.initScriptsFolder().ifPresentOrElse(initScriptsFolder -> {
            miniStack.withFileSystemBind(initScriptsFolder, "/etc/localstack/init/ready.d", BindMode.READ_ONLY);
        }, () -> config.initScriptsClasspath().ifPresent(resourcePath -> {
            // scripts must be executable but withClasspathResourceMapping will extract file with read only for regular file
            final MountableFile mountableFile = MountableFile.forClasspathResource(resourcePath, 555);
            miniStack.withCopyFileToContainer(mountableFile, "/etc/localstack/init/ready.d");
        }));

        config.initCompletionMsg().ifPresent(initCompletionMsg -> {
            miniStack.waitingFor(Wait.forLogMessage(".*" + initCompletionMsg + ".*\\n", 1));
        });

        ConfigureUtil.configureNetwork(miniStack, composeProjectBuildItem.getDefaultNetworkId(), useSharedNetwork, "ministack");

        miniStack.withEnv(config.containerProperties());
        requestedServices.stream().map(r -> r.getConfig().containerProperties()).forEach(miniStack::withEnv);

        configureSharedServiceLabel(miniStack, launchMode.getLaunchMode(), DEV_SERVICE_LABEL, config.serviceName());

        config.port().ifPresent(
                port -> miniStack.setPortBindings(Collections.singletonList("%s:%s".formatted(port, MINISTACK_PORT))));

        return new StartableContainer<>(miniStack, MiniStackContainer::getEndpoint);
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
                return "ministack";
            }

            @Override
            public String getSecretKey() {
                return "ministack";
            }
        };
    }
}
