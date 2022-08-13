package io.quarkus.amazon.common.deployment;

import java.io.Closeable;
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
import io.quarkus.devservices.common.ContainerShutdownCloseable;
import io.quarkus.runtime.LaunchMode;

public class DevServicesLocalStackProcessor {

    private static final Logger log = Logger.getLogger(DevServicesLocalStackProcessor.class);

    static volatile List<RunningDevService> devServices;

    static volatile boolean first = true;

    static final String DEV_SERVICE_LABEL = "quarkus-dev-service-localstack";

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
            // reuse same container for service with same shared name
            requestedServicesBySharedServiceName = requestedServices
                    .stream()
                    .filter(rs -> rs.getSharedConfig().isShared())
                    .collect(Collectors.toMap(r -> r.getSharedConfig().getServiceName(),
                            Collections::singletonList,
                            (requestedService1, requestedService2) -> {
                                List<DevServicesLocalStackProviderBuildItem> ret = new ArrayList<>();
                                ret.addAll(requestedService1);
                                ret.addAll(requestedService2);
                                return ret;
                            }));

            // collect non shared services in one separate container
            requestedServicesBySharedServiceName.put("aws",
                    requestedServices.stream().filter(rs -> !rs.getSharedConfig().isShared())
                            .collect(Collectors.toList()));
        } else {
            requestedServicesBySharedServiceName = requestedServices
                    .stream()
                    .collect(Collectors.toMap(
                            r -> String.format("%s-%s", r.getSharedConfig().getServiceName(), r.getService().getName()),
                            Collections::singletonList,
                            (requestedService1, requestedService2) -> {
                                List<DevServicesLocalStackProviderBuildItem> ret = new ArrayList<>();
                                ret.addAll(requestedService1);
                                ret.addAll(requestedService2);
                                return ret;
                            }));
        }

        List<RunningDevService> runningDevServices = new ArrayList<>();

        requestedServicesBySharedServiceName.forEach((key, value) -> {
            RunningDevService namedDevService = startLocalStack(key,
                    dockerStatusBuildItem, launchMode.getLaunchMode(),
                    localStackDevServicesBuildTimeConfig, value,
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

    private RunningDevService startLocalStack(String serviceName, DockerStatusBuildItem dockerStatusBuildItem,
            LaunchMode launchMode,
            LocalStackDevServicesBuildTimeConfig localStackDevServicesBuildTimeConfig,
            List<DevServicesLocalStackProviderBuildItem> requestedServices,
            boolean useSharedNetwork,
            Optional<Duration> timeout,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem) {

        // no service requested
        if (requestedServices.size() == 0)
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
                requestedServices.stream().map(ds -> ds.getService().getName()).toArray(String[]::new));
        String containerFriendlyName = serviceName + " (" + prettyRequestServicesName + ")";
        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode == LaunchMode.TEST ? "(test) " : "") + "Amazon Dev Services for " + containerFriendlyName
                        + " starting:",
                consoleInstalledBuildItem,
                loggingSetupBuildItem);

        try {
            LocalStackContainer container = new LocalStackContainer(
                    DockerImageName.parse(localStackDevServicesBuildTimeConfig.imageName))
                    .withEnv(
                            Stream.concat(
                                    requestedServices.stream().map(ds -> ds.getEnv())
                                            .flatMap(ds -> ds.entrySet().stream()),
                                    localStackDevServicesBuildTimeConfig.containerProperties.entrySet().stream())
                                    .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue())))
                    .withServices(requestedServices.stream().map(ds -> ds.getService()).toArray(EnabledService[]::new))
                    .withLabel(DEV_SERVICE_LABEL, serviceName);
            ConfigureUtil.configureSharedNetwork(container, serviceName);

            timeout.ifPresent(container::withStartupTimeout);

            container.start();

            Map<String, String> map = new HashMap<>();
            requestedServices.forEach(ds -> {
                map.putAll(ds.getDevProvider().prepareLocalStack(container));
            });

            compressor.close();
            log.info("Amazon Dev Services for " + containerFriendlyName + " started.");

            return new RunningDevService(serviceName, container.getContainerId(),
                    new ContainerShutdownCloseable(container, containerFriendlyName), map);

        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }
    }
}
