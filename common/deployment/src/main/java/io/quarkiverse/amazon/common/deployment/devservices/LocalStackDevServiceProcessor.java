package io.quarkiverse.amazon.common.deployment.devservices;

import static io.quarkus.devservices.common.ConfigureUtil.configureSharedServiceLabel;
import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.EnabledService;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import io.quarkiverse.amazon.common.deployment.spi.AwsStackContainer;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.AwsStackDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.LocalStackDevServicesBuildTimeConfig;
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
 * Processor for LocalStack dev services.
 */
@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class,
        DevServicesConfig.Enabled.class }, onlyIfNot = LocalStackDevServicesBuildTimeConfig.LegacyModeEnabled.class)
public class LocalStackDevServiceProcessor extends AbstractDevServicesAwsStackProcessor {

    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-localstack";
    private static final String FEATURE_NAME = "amazon-localstack";
    private static final int LOCALSTACK_PORT = 4566;

    private static final ContainerLocator localStackContainerLocator = locateContainerWithLabels(LOCALSTACK_PORT,
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
        return LOCALSTACK_PORT;
    }

    @Override
    protected ContainerLocator getContainerLocator() {
        return localStackContainerLocator;
    }

    @BuildStep
    public DevServicesResultBuildItem startLocalStackDevService(
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem compose,
            LaunchModeBuildItem launchMode,
            List<DevServicesSharedNetworkBuildItem> sharedNetwork,
            DevServicesConfig devServicesConfig,
            GlobalDevServicesBuildTimeConfig globalConfig,
            LocalStackDevServicesBuildTimeConfig config,
            List<DevServicesAwsStackProviderBuildItem> requestedServices) {

        var requestedLocalStackServices = requestedServices
                .stream()
                .filter(rs -> rs.getConfig().provider().orElse(globalConfig.provider())
                        .equals(GlobalDevServicesBuildTimeConfig.AwsStack.LOCALSTACK))
                .collect(Collectors.toList());

        return startAwsStackDevService(dockerStatusBuildItem, compose, launchMode, sharedNetwork, devServicesConfig,
                globalConfig, config,
                requestedLocalStackServices);
    }

    @Override
    protected Startable createContainer(DevServicesComposeProjectBuildItem composeProjectBuildItem,
            AwsStackDevServicesBuildTimeConfig config, boolean useSharedNetwork,
            LaunchModeBuildItem launchMode, List<DevServicesAwsStackProviderBuildItem> requestedServices) {
        LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse(config.imageName()));

        config.initScriptsFolder().ifPresentOrElse(initScriptsFolder -> {
            localStack.withFileSystemBind(initScriptsFolder, "/etc/localstack/init/ready.d", BindMode.READ_ONLY);
            localStack.addEnv("LOCALSTACK_HOST", "127.0.0.1");
        }, () -> config.initScriptsClasspath().ifPresent(resourcePath -> {
            // scripts must be executable but withClasspathResourceMapping will extract file with read only for regular file
            final MountableFile mountableFile = MountableFile.forClasspathResource(resourcePath, 555);
            localStack.withCopyFileToContainer(mountableFile, "/etc/localstack/init/ready.d");
            localStack.addEnv("LOCALSTACK_HOST", "127.0.0.1");
        }));

        config.initCompletionMsg().ifPresent(initCompletionMsg -> {
            localStack.waitingFor(Wait.forLogMessage(".*" + initCompletionMsg + ".*\\n", 1));
        });

        ConfigureUtil.configureNetwork(localStack, composeProjectBuildItem.getDefaultNetworkId(), useSharedNetwork,
                "localstack");

        localStack.withEnv(config.containerProperties());
        requestedServices.stream().map(r -> r.getConfig().containerProperties()).forEach(localStack::withEnv);

        configureSharedServiceLabel(localStack, launchMode.getLaunchMode(), DEV_SERVICE_LABEL, config.serviceName());

        config.port().ifPresent(
                port -> localStack.setPortBindings(Collections.singletonList("%s:%s".formatted(port, LOCALSTACK_PORT))));

        return new StartableContainer<>(localStack, c -> c.getEndpoint().toString());
    }

    @Override
    protected AwsStackContainer createAwsStackContainerFromEndpoint(String endpoint) {
        try (LocalStackContainer localStack = new LocalStackContainer("latest")) {
            var region = localStack.getRegion();
            var accessKey = localStack.getAccessKey();
            var secretKey = localStack.getSecretKey();
            return new AwsStackContainer() {
                @Override
                public URI getEndpoint() {
                    return URI.create(endpoint);
                }

                @Override
                public String getRegion() {
                    return region;
                }

                @Override
                public String getAccessKey() {
                    return accessKey;
                }

                @Override
                public String getSecretKey() {
                    return secretKey;
                }
            };
        }
    }

    /**
     * Returns the property configuration name for the given {@link LocalStackContainer.EnabledService}.
     * <p>
     * The property configuration name is the name of the service, which is the same as the AWSSDK artifact id.
     * The only exception is the Step Functions service, which is named "sfn" in the AWSSDK, "stepfunctions"
     * and "logs" in the LocalStack configuration.
     * <p>
     *
     * @param enabledService the LocalStack enabled service
     * @return the property configuration name
     */
    protected String getPropertyConfigurationName(LocalStackContainer.EnabledService enabledService) {
        if (enabledService == LocalStackContainer.Service.STEPFUNCTIONS)
            return "sfn";
        if (enabledService.getName().equals("events"))
            return "eventbridge";
        if (enabledService == LocalStackContainer.Service.CLOUDWATCHLOGS)
            return "cloudwatchlogs";
        if (enabledService.getName().equals("scheduler"))
            return "eventbridge-scheduler";
        return enabledService.getName();
    }
}
