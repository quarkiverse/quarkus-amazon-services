package io.quarkus.amazon.common.deployment;

import java.io.Closeable;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.EnabledService;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkus.amazon.common.deployment.spi.SharedLocalStackContainer;
import io.quarkus.amazon.common.runtime.LocalStackDevServicesBuildTimeConfig;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunningDevService;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerAddress;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.devservices.common.ContainerShutdownCloseable;
import io.quarkus.runtime.LaunchMode;

public class DevServicesLocalStackProcessor {

    private static final Logger log = Logger.getLogger(DevServicesLocalStackProcessor.class);

    static volatile List<RunningDevService> devServices;

    static volatile boolean first = true;

    static final String DEV_SERVICE_LABEL = "quarkus-dev-service-localstack";

    // Since version 0.11, LocalStack exposes all services on the same port
    private static final int PORT = EnabledService.named("whatever").getPort();

    private static final ContainerLocator containerLocator = new ContainerLocator(DEV_SERVICE_LABEL, PORT);

    @BuildStep(onlyIfNot = IsNormal.class, onlyIf = GlobalDevServicesConfig.Enabled.class)
    public void startLocalStackDevService(
            LaunchModeBuildItem launchMode,
            LocalStackDevServicesBuildTimeConfig localStackDevServicesBuildTimeConfig,
            List<DevServicesLocalStackProviderBuildItem> requestedServices,
            DockerStatusBuildItem dockerStatusBuildItem,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            GlobalDevServicesConfig devServicesConfig,
            BuildProducer<DevServicesResultBuildItem> devServicesResultBuildItemBuildProducer,
            LoggingSetupBuildItem loggingSetupBuildItem) {

        if (devServices != null) {
            restartIfRequired();
        }

        Map<String, List<DevServicesLocalStackProviderBuildItem>> requestedServicesBySharedServiceName;
        if (launchMode.isTest()) {
            // reuse same container for service with same service name
            // the value of the label is the service name (eg. "default")
            requestedServicesBySharedServiceName = requestedServices
                    .stream()
                    .collect(Collectors.toMap(r -> r.getSharedConfig().getServiceName(),
                            Collections::singletonList,
                            (requestedService1, requestedService2) -> {
                                List<DevServicesLocalStackProviderBuildItem> ret = new ArrayList<>();
                                ret.addAll(requestedService1);
                                ret.addAll(requestedService2);
                                return ret;
                            }));
        } else {
            // in dev mode, each shared service will be instanciated in one container
            // so that it is easier to reuse a container accross different applications
            // the value of the label is service-name property dash service name (eg. "default-s3")
            requestedServicesBySharedServiceName = requestedServices
                    .stream()
                    .filter(rs -> rs.getSharedConfig().isShared())
                    .collect(Collectors.toMap(
                            r -> String.format("%s-%s", r.getSharedConfig().getServiceName(), r.getService().getName()),
                            Collections::singletonList,
                            (requestedService1, requestedService2) -> {
                                List<DevServicesLocalStackProviderBuildItem> ret = new ArrayList<>();
                                ret.addAll(requestedService1);
                                ret.addAll(requestedService2);
                                return ret;
                            }));

            // group non shared service by service name
            requestedServicesBySharedServiceName = requestedServices
                    .stream()
                    .filter(rs -> !rs.getSharedConfig().isShared())
                    .collect(Collectors.toMap(r -> r.getSharedConfig().getServiceName(),
                            Collections::singletonList,
                            (requestedService1, requestedService2) -> {
                                List<DevServicesLocalStackProviderBuildItem> ret = new ArrayList<>();
                                ret.addAll(requestedService1);
                                ret.addAll(requestedService2);
                                return ret;
                            }));

        }

        List<RunningDevService> runningDevServices = new ArrayList<>();

        requestedServicesBySharedServiceName.forEach((devServiceName, requestedServicesGroup) -> {
            RunningDevService namedDevService = startLocalStack(devServiceName,
                    dockerStatusBuildItem, launchMode.getLaunchMode(),
                    localStackDevServicesBuildTimeConfig, requestedServicesGroup,
                    !devServicesSharedNetworkBuildItem.isEmpty(),
                    devServicesConfig.timeout,
                    consoleInstalledBuildItem,
                    loggingSetupBuildItem);
            if (namedDevService != null) {
                runningDevServices.add(namedDevService);
            }
        });

        // Configure the watch dog
        if (first) {
            first = false;
            Runnable closeTask = new Runnable() {
                @Override
                public void run() {
                    if (devServices != null) {
                        for (Closeable i : devServices) {
                            try {
                                i.close();
                            } catch (Throwable t) {
                                log.error("Failed to stop aws broker", t);
                            }
                        }
                    }
                    first = true;
                    devServices = null;
                }
            };
            closeBuildItem.addCloseTask(closeTask, true);
        }
        devServices = runningDevServices;

        devServices.forEach(devService -> devServicesResultBuildItemBuildProducer.produce(devService.toBuildItem()));
    }

    private void restartIfRequired() {
        // always restart for now
        for (Closeable i : devServices) {
            try {
                i.close();
            } catch (Throwable e) {
                log.error("Failed to stop aws services", e);
            }
        }
        devServices = null;
    }

    private RunningDevService startLocalStack(String devServiceName, DockerStatusBuildItem dockerStatusBuildItem,
            LaunchMode launchMode,
            LocalStackDevServicesBuildTimeConfig localStackDevServicesBuildTimeConfig,
            List<DevServicesLocalStackProviderBuildItem> requestedServicesGroup,
            boolean useSharedNetwork,
            Optional<Duration> timeout,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem) {

        // no service requested
        if (requestedServicesGroup.size() == 0)
            return null;

        if (!dockerStatusBuildItem.isDockerAvailable()) {
            String message = "Docker isn't working, dev services for Amazon Services is not available.";
            if (launchMode == LaunchMode.TEST) {
                throw new IllegalStateException(message);
            } else {
                // in dev-mode we just want to warn users and allow them to recover
                log.warn(message);
                return null;
            }
        }

        String prettyRequestServicesName = String.join(", ",
                requestedServicesGroup.stream().map(ds -> ds.getService().getName()).toArray(String[]::new));
        String containerFriendlyName = devServiceName + " (" + prettyRequestServicesName + ")";
        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode == LaunchMode.TEST ? "(test) " : "") + "Amazon Dev Services for " + containerFriendlyName
                        + " starting:",
                consoleInstalledBuildItem,
                loggingSetupBuildItem);
        Map<String, String> config = new HashMap<>();
        try {

            final Optional<ContainerAddress> maybeContainerAddress = containerLocator.locateContainer(devServiceName,
                    requestedServicesGroup.get(0).getSharedConfig().isShared(), launchMode);

            var devService = maybeContainerAddress.map(containerAddress -> {
                // LocalStack default values are not statically exposed
                // create an instance just to get those value
                LocalStackContainer defaultValueContainerNotStarted = new LocalStackContainer(
                        DockerImageName.parse(localStackDevServicesBuildTimeConfig.imageName));

                requestedServicesGroup.forEach(ds -> {
                    config.putAll(ds.getDevProvider().reuseLocalStack(new SharedLocalStackContainer() {
                        public URI getEndpointOverride(EnabledService enabledService) {
                            try {
                                return new URI("http://" + containerAddress.getHost() + ":" + containerAddress.getPort());
                            } catch (URISyntaxException e) {
                                throw new IllegalStateException("Cannot obtain endpoint URL", e);
                            }
                        }

                        public String getRegion() {
                            // DEFAULT_REGION env var can override default value and this is not supported
                            return defaultValueContainerNotStarted.getRegion();
                        }

                        public String getAccessKey() {
                            return defaultValueContainerNotStarted.getAccessKey();
                        }

                        public String getSecretKey() {
                            return defaultValueContainerNotStarted.getSecretKey();
                        }
                    }));
                });

                return new RunningDevService(devServiceName, containerAddress.getId(), null, config);
            }).orElseGet(
                    () -> {
                        LocalStackContainer container = new LocalStackContainer(
                                DockerImageName.parse(localStackDevServicesBuildTimeConfig.imageName))
                                .withEnv(
                                        Stream.concat(
                                                requestedServicesGroup.stream().map(ds -> ds.getEnv())
                                                        .flatMap(ds -> ds.entrySet().stream()),
                                                localStackDevServicesBuildTimeConfig.containerProperties.entrySet()
                                                        .stream())
                                                .collect(Collectors.toMap(entry -> entry.getKey(),
                                                        entry -> entry.getValue())))
                                .withServices(requestedServicesGroup.stream().map(ds -> ds.getService())
                                        .toArray(EnabledService[]::new))
                                .withLabel(DEV_SERVICE_LABEL, devServiceName);
                        ConfigureUtil.configureSharedNetwork(container, devServiceName);

                        timeout.ifPresent(container::withStartupTimeout);

                        container.start();

                        requestedServicesGroup.forEach(ds -> {
                            config.putAll(ds.getDevProvider().prepareLocalStack(container));
                        });

                        log.info("Amazon Dev Services for " + containerFriendlyName
                                + " started. Other Quarkus applications in dev mode will find "
                                + "the LocalStack automatically.");

                        return new RunningDevService(devServiceName, container.getContainerId(),
                                new ContainerShutdownCloseable(container, containerFriendlyName), config);
                    });

            compressor.close();
            return devService;

        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }
    }
}
