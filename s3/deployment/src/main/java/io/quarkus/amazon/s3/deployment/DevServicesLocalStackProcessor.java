package io.quarkus.amazon.s3.deployment;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.jboss.logging.Logger;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.amazon.s3.runtime.S3BuildTimeConfig;
import io.quarkus.amazon.s3.runtime.S3DevServicesBuildTimeConfig;
import io.quarkus.deployment.IsDockerWorking;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunningDevService;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerAddress;
import io.quarkus.devservices.common.ContainerLocator;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

/**
 * Starts a local stack container to emulate AWS services
 */
public class DevServicesLocalStackProcessor {

    private static final Logger log = Logger.getLogger(DevServicesLocalStackProcessor.class);

    static volatile RunningDevService devService;
    static volatile S3DevServiceCfg cfg;
    static volatile boolean first = true;

    static final String DEV_SERVICE_LABEL = "quarkus-dev-service-localstack-23";
    static final int PORT = 6298;

    private static final ContainerLocator containerLocator = new ContainerLocator(DEV_SERVICE_LABEL, PORT);

    private final IsDockerWorking isDockerWorking = new IsDockerWorking(true);

    @BuildStep(onlyIfNot = IsNormal.class, onlyIf = GlobalDevServicesConfig.Enabled.class)
    public DevServicesResultBuildItem startS3DevService(
            LaunchModeBuildItem launchMode,
            S3BuildTimeConfig s3ClientBuildTimeConfig,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem, GlobalDevServicesConfig devServicesConfig) throws URISyntaxException {

        S3DevServiceCfg configuration = getConfiguration(s3ClientBuildTimeConfig);

        if (devService != null) {
            boolean shouldShutdownTheBroker = !configuration.equals(cfg);
            if (!shouldShutdownTheBroker) {
                return devService.toBuildItem();
            }
            shutdownBroker();
            cfg = null;
        }

        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "S3 Dev Services Starting:",
                consoleInstalledBuildItem, loggingSetupBuildItem);
        try {
            devService = startS3(configuration, launchMode,
                    !devServicesSharedNetworkBuildItem.isEmpty(),
                    devServicesConfig.timeout);
            compressor.close();
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }

        if (devService == null) {
            return null;
        }

        // Configure the watch dog
        if (first) {
            first = false;
            Runnable closeTask = () -> {
                if (devService != null) {
                    shutdownBroker();
                }
                first = true;
                devService = null;
                cfg = null;
            };
            closeBuildItem.addCloseTask(closeTask, true);
        }
        cfg = configuration;

        createBuckets(configuration);
        return devService.toBuildItem();
    }

    public void createBuckets(S3DevServiceCfg configuration) throws URISyntaxException {
        try (S3Client client = S3Client.builder()
                .endpointOverride(new URI(devService.getConfig().get("quarkus.s3.endpoint-override")))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials
                        .create("accesskey", "secretkey")))
                .build()) {
            for (var i : configuration.buckets) {
                client.createBucket(CreateBucketRequest.builder().bucket(i).build());
            }
        }
    }

    private void shutdownBroker() {
        if (devService != null) {
            try {
                devService.close();
            } catch (Throwable e) {
                log.error("Failed to stop the S3 broker", e);
            } finally {
                devService = null;
            }
        }
    }

    private RunningDevService startS3(S3DevServiceCfg config,
            LaunchModeBuildItem launchMode, boolean useSharedNetwork, Optional<Duration> timeout) {
        if (!config.devServicesEnabled) {
            // explicitly disabled
            log.debug("Not starting dev services for S3, as it has been disabled in the config.");
            return null;
        }

        if (!isDockerWorking.getAsBoolean()) {
            log.warn(
                    "Docker isn't working, dev services for S3 is not availible.");
            return null;
        }

        final Optional<ContainerAddress> maybeContainerAddress = containerLocator.locateContainer(config.serviceName,
                config.shared,
                launchMode.getLaunchMode());
        if (maybeContainerAddress.isPresent()) {
        }

        LocalStackContainer container = new LocalStackContainer(DockerImageName.parse(config.imageName))
                .withServices(LocalStackContainer.Service.S3);
        if (config.serviceName != null) {
            container.withLabel(DevServicesLocalStackProcessor.DEV_SERVICE_LABEL, config.serviceName);
        }
        ConfigureUtil.configureSharedNetwork(container, "s3");
        container.start();

        String url = container.getEndpointOverride(LocalStackContainer.Service.S3).toASCIIString();
        return new RunningDevService("s3", container.getContainerId(), container::close, Map.of(
                "quarkus.s3.endpoint-override", url,
                "quarkus.s3.aws.region", "us-east-1",
                "quarkus.s3.aws.credentials.type", "static",
                "quarkus.s3.aws.credentials.static-provider.access-key-id", "accesskey",
                "quarkus.s3.aws.credentials.static-provider.secret-access-key", "secretkey"));
    }

    private S3DevServiceCfg getConfiguration(S3BuildTimeConfig cfg) {
        S3DevServicesBuildTimeConfig devServicesConfig = cfg.devservices;
        return new S3DevServiceCfg(devServicesConfig);
    }

    private static final class S3DevServiceCfg {
        private final boolean devServicesEnabled;
        private final String imageName;
        private final boolean shared;
        private final String serviceName;
        private final Set<String> buckets;

        public S3DevServiceCfg(S3DevServicesBuildTimeConfig config) {
            this.devServicesEnabled = config.enabled;
            this.imageName = config.imageName;
            this.shared = config.shared;
            this.serviceName = config.serviceName;
            this.buckets = config.buckets;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            S3DevServiceCfg that = (S3DevServiceCfg) o;
            return devServicesEnabled == that.devServicesEnabled && shared == that.shared
                    && Objects.equals(imageName, that.imageName) && Objects.equals(serviceName, that.serviceName)
                    && Objects.equals(buckets, that.buckets);
        }

        @Override
        public int hashCode() {
            return Objects.hash(devServicesEnabled, imageName, shared, serviceName, buckets);
        }
    }

}
