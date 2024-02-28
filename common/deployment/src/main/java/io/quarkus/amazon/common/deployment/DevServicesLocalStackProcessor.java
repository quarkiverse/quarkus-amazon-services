package io.quarkus.amazon.common.deployment;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.EnabledService;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import io.quarkus.amazon.common.deployment.spi.BorrowedLocalStackContainer;
import io.quarkus.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkus.amazon.common.deployment.spi.LocalStackDevServicesBaseConfig;
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

    static volatile List<RunningDevServiceWithConfig> currentDevServices;

    // global LocalStack configuration, if changed all containers will restart
    static volatile LocalStackDevServicesConfig currentLocalStackDevServicesConfig;

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

        Map<String, List<DevServicesLocalStackProviderBuildItem>> requestedServicesBySharedServiceName;
        if (launchMode.isTest()) {
            // reuse same container for service with same service name
            // the value of the label is the service name (eg. "default")
            requestedServicesBySharedServiceName = requestedServices
                    .stream()
                    .collect(Collectors.toMap(r -> r.getConfig().getServiceName(),
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
            // the value of the label is service-name property dash service name (eg.
            // "default-s3")
            requestedServicesBySharedServiceName = requestedServices
                    .stream()
                    .filter(rs -> rs.getConfig().isShared())
                    .collect(Collectors.toMap(
                            r -> r.getConfig().isIsolated()
                                    ? String.format("%s-%s", r.getConfig().getServiceName(), r.getService().getName())
                                    : r.getConfig().getServiceName(),
                            Collections::singletonList,
                            (requestedService1, requestedService2) -> {
                                List<DevServicesLocalStackProviderBuildItem> ret = new ArrayList<>();
                                ret.addAll(requestedService1);
                                ret.addAll(requestedService2);
                                return ret;
                            }));

            // group non shared service by service name
            requestedServicesBySharedServiceName.putAll(requestedServices
                    .stream()
                    .filter(rs -> !rs.getConfig().isShared())
                    .collect(Collectors.toMap(r -> r.getConfig().getServiceName(),
                            Collections::singletonList,
                            (requestedService1, requestedService2) -> {
                                List<DevServicesLocalStackProviderBuildItem> ret = new ArrayList<>();
                                ret.addAll(requestedService1);
                                ret.addAll(requestedService2);
                                return ret;
                            })));

        }

        if (!requestedServicesBySharedServiceName.isEmpty() && !dockerStatusBuildItem.isDockerAvailable()) {
            String message = "Docker isn't working, dev services for Amazon Services is not available.";
            if (launchMode.getLaunchMode() == LaunchMode.TEST) {
                throw new IllegalStateException(message);
            } else {
                // in dev-mode we just want to warn users and allow them to recover
                log.warn(message);
                return;
            }
        }

        LocalStackDevServicesConfig newlocalStackDevServicesConfig = LocalStackDevServicesConfig
                .from(localStackDevServicesBuildTimeConfig);
        List<RunningDevServiceWithConfig> newRunningDevServices = new ArrayList<>();

        if (currentDevServices != null) {
            stopOrRestartIfRequired(newlocalStackDevServicesConfig, requestedServicesBySharedServiceName);
            newRunningDevServices.addAll(currentDevServices);
        }

        // start new or modified container
        requestedServicesBySharedServiceName.forEach((devServiceName, requestedServicesGroup) -> {
            RunningDevService namedDevService = startLocalStack(devServiceName,
                    launchMode.getLaunchMode(),
                    localStackDevServicesBuildTimeConfig, requestedServicesGroup,
                    !devServicesSharedNetworkBuildItem.isEmpty(),
                    devServicesConfig.timeout,
                    consoleInstalledBuildItem,
                    loggingSetupBuildItem);
            if (namedDevService != null) {
                newRunningDevServices.add(new RunningDevServiceWithConfig(namedDevService, requestedServicesGroup));
            }
        });

        currentLocalStackDevServicesConfig = newlocalStackDevServicesConfig;
        currentDevServices = newRunningDevServices;
        currentDevServices
                .forEach(devService -> devServicesResultBuildItemBuildProducer
                        .produce(devService.getRunningDevService().toBuildItem()));

        // Configure the watch dog
        if (first) {
            first = false;
            Runnable closeTask = new Runnable() {
                @Override
                public void run() {
                    if (currentDevServices != null) {
                        for (RunningDevServiceWithConfig i : currentDevServices) {
                            try {
                                i.getRunningDevService().close();
                            } catch (Throwable t) {
                                log.error("Failed to stop aws broker", t);
                            }
                        }
                    }
                    first = true;
                    currentDevServices = null;
                }
            };
            closeBuildItem.addCloseTask(closeTask, true);
        }
    }

    private void stopOrRestartIfRequired(
            LocalStackDevServicesConfig newlocalStackDevServicesConfig,
            Map<String, List<DevServicesLocalStackProviderBuildItem>> requestedServicesBySharedServiceName) {

        boolean preMatchCondition = newlocalStackDevServicesConfig.equals(currentLocalStackDevServicesConfig);

        List<RunningDevServiceWithConfig> keptRunningService = new ArrayList<>();
        // Remove from the input list requested containers
        // that remains active if the config still match the previous one
        for (RunningDevServiceWithConfig i : currentDevServices) {
            var runningDevService = i.getRunningDevService();
            var oldContainerName = runningDevService.getName();
            var oldDevServiceConfig = i.getConfig();
            var maybeNewDevService = requestedServicesBySharedServiceName.get(oldContainerName);
            if (maybeNewDevService == null || !preMatchCondition || !match(oldDevServiceConfig, maybeNewDevService)) {
                try {
                    runningDevService.close();
                } catch (Throwable e) {
                    log.error("Failed to stop aws services", e);
                }
            } else {
                // keep the running dev service container
                requestedServicesBySharedServiceName.remove(oldContainerName);
                keptRunningService.add(i);
            }
        }

        currentDevServices = keptRunningService;
    }

    private boolean match(Map<String, LocalStackDevServicesBaseConfig> oldDevServiceConfig,
            List<DevServicesLocalStackProviderBuildItem> newDevService) {

        return newDevService.stream().collect(Collectors.toMap(r -> r.getService().getName(), r -> r.getConfig()))
                .equals(oldDevServiceConfig);

    }

    private RunningDevService startLocalStack(String devServiceName,
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
                    requestedServicesGroup.get(0).getConfig().isShared(), launchMode);

            var devService = maybeContainerAddress.map(containerAddress -> {
                // LocalStack default values are not statically exposed
                // create an instance just to get those value
                LocalStackContainer defaultValueContainerNotStarted = new LocalStackContainer(
                        DockerImageName.parse(localStackDevServicesBuildTimeConfig.imageName())
                                .asCompatibleSubstituteFor("localstack/localstack"));

                requestedServicesGroup.forEach(ds -> {
                    config.putAll(ds.getDevProvider().reuseLocalStack(new BorrowedLocalStackContainer() {
                        public URI getEndpointOverride(EnabledService enabledService) {
                            try {
                                return new URI(
                                        "http://" + containerAddress.getHost() + ":" + containerAddress.getPort());
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
                                DockerImageName.parse(localStackDevServicesBuildTimeConfig.imageName())
                                        .asCompatibleSubstituteFor("localstack/localstack"))
                                .withEnv(
                                        Stream.concat(
                                                requestedServicesGroup.stream()
                                                        .map(ds -> ds.getConfig().getContainerProperties())
                                                        .flatMap(ds -> ds.entrySet().stream()),
                                                localStackDevServicesBuildTimeConfig.containerProperties().entrySet()
                                                        .stream())
                                                .collect(Collectors.toMap(entry -> entry.getKey(),
                                                        entry -> entry.getValue())))
                                .withServices(requestedServicesGroup.stream().map(ds -> ds.getService())
                                        .toArray(EnabledService[]::new))
                                .withLabel(DEV_SERVICE_LABEL, devServiceName);

                        localStackDevServicesBuildTimeConfig.port().ifPresent(port ->
                            container.setPortBindings(Collections.singletonList("%s:%s".formatted(port, PORT)))
                        );

                        localStackDevServicesBuildTimeConfig.initScriptsFolder().ifPresentOrElse(initScriptsFolder -> {
                            container.withFileSystemBind(initScriptsFolder, "/etc/localstack/init/ready.d", BindMode.READ_ONLY);
                        }, () -> localStackDevServicesBuildTimeConfig.initScriptsClasspath().ifPresent(resourcePath -> {
                            // scripts must be executable but withClasspathResourceMapping will extract file with read only for regular file
                            final MountableFile mountableFile = MountableFile.forClasspathResource(resourcePath, 555);
                            container.withCopyFileToContainer(mountableFile, "/etc/localstack/init/ready.d");
                        }));

                        localStackDevServicesBuildTimeConfig.initCompletionMsg().ifPresent(initCompletionMsg -> {
                            container.waitingFor(Wait.forLogMessage(".*" + initCompletionMsg + ".*\\n", 1));
                        });

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

    private static final class LocalStackDevServicesConfig {
        private final String imageName;
        private final Map<String, String> containerProperties;

        public static LocalStackDevServicesConfig from(LocalStackDevServicesBuildTimeConfig config) {
            return new LocalStackDevServicesConfig(config);
        }

        private LocalStackDevServicesConfig(LocalStackDevServicesBuildTimeConfig config) {
            this.imageName = config.imageName();
            this.containerProperties = config.containerProperties();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            LocalStackDevServicesConfig that = (LocalStackDevServicesConfig) o;
            return Objects.equals(imageName, that.imageName)
                    && Objects.equals(containerProperties, that.containerProperties);
        }

        @Override
        public int hashCode() {
            return Objects.hash(imageName, containerProperties);
        }
    }

    private static final class RunningDevServiceWithConfig {

        private final RunningDevService runningDevService;

        private final Map<String, LocalStackDevServicesBaseConfig> config;

        public RunningDevServiceWithConfig(RunningDevService namedDevService,
                List<DevServicesLocalStackProviderBuildItem> requestedServicesGroup) {
            this.runningDevService = namedDevService;
            this.config = requestedServicesGroup.stream()
                    .collect(Collectors.toMap(r -> r.getService().getName(), r -> r.getConfig()));
        }

        public RunningDevService getRunningDevService() {
            return runningDevService;
        }

        public Map<String, LocalStackDevServicesBaseConfig> getConfig() {
            return config;
        }
    }
}
