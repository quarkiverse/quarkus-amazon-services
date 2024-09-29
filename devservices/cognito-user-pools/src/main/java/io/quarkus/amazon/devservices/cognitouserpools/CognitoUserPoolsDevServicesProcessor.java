package io.quarkus.amazon.devservices.cognitouserpools;

import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.amazon.cognitouserpools.runtime.CognitoUserPoolsBuildTimeConfig;
import io.quarkus.amazon.common.runtime.AwsCredentialsProviderType;
import io.quarkus.amazon.common.runtime.MotoDevServicesBuildTimeConfig;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunningDevService;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

public class CognitoUserPoolsDevServicesProcessor {

    private static final Logger log = Logger.getLogger(CognitoUserPoolsDevServicesProcessor.class);

    static volatile RunningDevService devServices;

    @BuildStep(onlyIfNot = IsNormal.class, onlyIf = GlobalDevServicesConfig.Enabled.class)
    DevServicesResultBuildItem setupCognitoUserPools(
            CognitoUserPoolsBuildTimeConfig clientBuildTimeConfig,
            MotoDevServicesBuildTimeConfig motoDevServicesBuildTimeConfig,
            DockerStatusBuildItem dockerStatusBuildItem,
            LaunchModeBuildItem launchMode,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem) {

        if (devServices != null) {
            return devServices.toBuildItem();
        }

        // explicitly disabled
        if (!clientBuildTimeConfig.devservices().enabled().orElse(true)) {
            log.debugf(
                    "Not starting Dev Services for Amazon Services - cognito user pools, as it has been disabled in the config.");
            return null;
        }

        if (ConfigUtils.isPropertyPresent("quarkus.cognito-user-pools.endpoint-override")) {
            log.debugf(
                    "Not starting Dev Services for Amazon Services - cognito user pools, the quarkus.cognito-user-pools.endpoint-override is configured.");
            return null;
        }

        devServices = startMoto(dockerStatusBuildItem, launchMode.getLaunchMode(),
                motoDevServicesBuildTimeConfig,
                consoleInstalledBuildItem,
                loggingSetupBuildItem);

        return devServices.toBuildItem();
    }

    private RunningDevService startMoto(DockerStatusBuildItem dockerStatusBuildItem,
            LaunchMode launchMode,
            MotoDevServicesBuildTimeConfig motoDevServicesBuildTimeConfig,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem) {

        String containerFriendlyName = "cognito user pools";

        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode == LaunchMode.TEST ? "(test) " : "") + "Amazon Dev Services for cognito"
                        + containerFriendlyName
                        + " starting:",
                consoleInstalledBuildItem,
                loggingSetupBuildItem);

        try {
            MotoContainer container = new MotoContainer(
                    DockerImageName.parse(motoDevServicesBuildTimeConfig.imageName()))
                    .withEnv(motoDevServicesBuildTimeConfig.containerProperties());
            container.start();

            compressor.close();
            log.info("Amazon Dev Services for " + containerFriendlyName + " started.");

            return new RunningDevService(containerFriendlyName, container.getContainerId(),
                    container::close,
                    Map.of("quarkus.cognito-user-pools.endpoint-override", container.getEndpointOverride().toString(),
                            "quarkus.cognito-user-pools.aws.region", "us-east-1",
                            "quarkus.cognito-user-pools.aws.credentials.type", AwsCredentialsProviderType.STATIC.name(),
                            "quarkus.cognito-user-pools.aws.credentials.static-provider.access-key-id", "testing",
                            "quarkus.cognito-user-pools.aws.credentials.static-provider.secret-access-key", "testing"));
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }
    }
}
