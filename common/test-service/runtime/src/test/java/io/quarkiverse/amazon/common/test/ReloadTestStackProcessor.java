package io.quarkiverse.amazon.common.test;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;

import io.quarkiverse.amazon.common.deployment.devservices.AbstractDevServicesAwsStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.AwsStackContainer;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.AwsStackDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.DevServicesAdditionalConfigBuildItem;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.Startable;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.devservices.common.StartableContainer;

/** Exercises the production stack lifecycle, replacing only the concrete container and connection metadata. */
@BuildSteps(onlyIf = IsDevServicesSupportedByLaunchMode.class)
public class ReloadTestStackProcessor extends AbstractDevServicesAwsStackProcessor {
    private static final String LABEL = "quarkus-dev-service-test-aws-stack";
    private static final int PORT = 8080;
    private static final ContainerLocator LOCATOR = ContainerLocator.locateContainerWithLabels(PORT, LABEL);

    @BuildStep
    DevServicesResultBuildItem start(DockerStatusBuildItem dockerStatus, DevServicesComposeProjectBuildItem compose,
            LaunchModeBuildItem launchMode, List<DevServicesSharedNetworkBuildItem> sharedNetwork,
            DevServicesConfig devServicesConfig, GlobalDevServicesBuildTimeConfig globalConfig,
            List<DevServicesAwsStackProviderBuildItem> requests,
            BuildProducer<DevServicesAdditionalConfigBuildItem> additionalConfig) {
        return startAwsStackDevService(dockerStatus, compose, launchMode, sharedNetwork, devServicesConfig,
                globalConfig, new TestStackConfig(), requests, additionalConfig);
    }

    @Override
    protected String getFeatureName() {
        return "test-aws-stack";
    }

    @Override
    protected String getDevServiceLabel() {
        return LABEL;
    }

    @Override
    protected int getStackPort() {
        return PORT;
    }

    @Override
    protected ContainerLocator getContainerLocator() {
        return LOCATOR;
    }

    @Override
    protected Startable createContainer(DevServicesComposeProjectBuildItem compose, AwsStackDevServicesBuildTimeConfig config,
            boolean useSharedNetwork, LaunchModeBuildItem launchMode, List<DevServicesAwsStackProviderBuildItem> requests) {
        var container = new GenericContainer<>(config.imageName())
                .withExposedPorts(PORT)
                .withLabel(LABEL, config.serviceName())
                .withCopyToContainer(Transferable.of("""
                        events {}
                        http {
                            server {
                                listen 8080;
                                location / {
                                    default_type application/x-amz-json-1.1;
                                    return 200 '{}';
                                }
                            }
                        }
                        """), "/etc/nginx/nginx.conf")
                .waitingFor(Wait.forHttp("/").forPort(PORT));
        return new StartableContainer<>(container,
                running -> "http://" + running.getHost() + ":" + running.getMappedPort(PORT));
    }

    @Override
    protected AwsStackContainer createAwsStackContainerFromEndpoint(String endpoint) {
        return new Stack(URI.create(endpoint));
    }

    @Override
    protected Map<String, String> connectionConfig(String endpointKey, Startable stack) {
        var config = new HashMap<>(super.connectionConfig(endpointKey, stack));
        config.put("test.aws.container-id", stack.getContainerId());
        return config;
    }

    public record TestStackConfig() implements AwsStackDevServicesBuildTimeConfig {
        // The fixture class is reloaded; compare settings through the stable SPI, not record class identity.
        @Override
        public boolean equals(Object other) {
            return other instanceof AwsStackDevServicesBuildTimeConfig config
                    && getClass().getName().equals(other.getClass().getName())
                    && imageName().equals(config.imageName()) && serviceName().equals(config.serviceName())
                    && enabled().equals(config.enabled()) && shared().equals(config.shared())
                    && initScriptsFolder().equals(config.initScriptsFolder())
                    && initScriptsClasspath().equals(config.initScriptsClasspath())
                    && initCompletionMsg().equals(config.initCompletionMsg())
                    && additionalServices().equals(config.additionalServices())
                    && containerProperties().equals(config.containerProperties()) && port().equals(config.port());
        }

        @Override
        public int hashCode() {
            return Objects.hash(imageName(), serviceName(), enabled(), shared(), initScriptsFolder(), initScriptsClasspath(),
                    initCompletionMsg(), additionalServices(), containerProperties(), port());
        }

        @Override
        public String imageName() {
            return "nginx:1.28-alpine";
        }

        @Override
        public String serviceName() {
            return "test-aws-stack";
        }

        @Override
        public Optional<Boolean> enabled() {
            return Optional.of(true);
        }

        @Override
        public Optional<Boolean> shared() {
            return Optional.of(false);
        }

        @Override
        public Optional<String> initScriptsFolder() {
            return Optional.empty();
        }

        @Override
        public Optional<String> initScriptsClasspath() {
            return Optional.empty();
        }

        @Override
        public Optional<String> initCompletionMsg() {
            return Optional.empty();
        }

        @Override
        public Map<String, DevServicesBuildTimeConfig> additionalServices() {
            return Map.of();
        }

        @Override
        public Map<String, String> containerProperties() {
            return Map.of();
        }

        @Override
        public Optional<Integer> port() {
            return Optional.empty();
        }
    }

    public record Stack(URI endpoint) implements AwsStackContainer {
        @Override
        public URI getEndpoint() {
            return endpoint;
        }

        @Override
        public String getRegion() {
            return "us-east-1";
        }

        @Override
        public String getAccessKey() {
            return "test";
        }

        @Override
        public String getSecretKey() {
            return "test";
        }
    }
}
