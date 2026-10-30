package io.quarkiverse.amazon.common.deployment.devservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import io.quarkiverse.amazon.common.deployment.spi.AwsStackContainer;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackExtensionProvider;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.FlociDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig.AwsStack;
import io.quarkus.deployment.builditem.DevServicesRegistryBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.Startable;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.runtime.LaunchMode;
import io.smallrye.config.SmallRyeConfigBuilder;

class DevServicesClientConfigReloadTest {
    private record Settings(Optional<Boolean> enabled, Map<String, String> containerProperties,
            Optional<AwsStack> provider) implements DevServicesBuildTimeConfig {
    }

    @Test
    void recalculatesClientPropertiesWithoutRestartingTheStack() {
        String prefix = "test.clients";
        var config = new SmallRyeConfigBuilder().withMapping(DevServicesConfig.class)
                .withMapping(GlobalDevServicesBuildTimeConfig.class).withMapping(FlociDevServicesBuildTimeConfig.class).build();
        var registry = new DevServicesRegistryBuildItem(UUID.randomUUID(), config.getConfigMapping(DevServicesConfig.class),
                LaunchMode.DEVELOPMENT);
        AtomicInteger starts = new AtomicInteger();
        AtomicInteger closes = new AtomicInteger();
        Startable stack = new Startable() {
            public void start() {
                starts.incrementAndGet();
            }

            public void close() {
                closes.incrementAndGet();
            }

            public String getConnectionInfo() {
                return "http://localhost:4566";
            }

            public String getContainerId() {
                return "client-config-reload";
            }
        };
        var settings = new Settings(Optional.of(true), Map.of(), Optional.of(AwsStack.FLOCI));
        try {
            for (String name : List.of("first", "second")) {
                var provider = new DevServicesAwsStackExtensionProvider() {
                    public void prepareAwsStackContainer(AwsStackContainer container) {
                    }

                    public void reuseAwsStackContainer(AwsStackContainer container) {
                    }

                    public Function<AwsStackContainer, Map<String, String>> getClientConfig() {
                        return container -> Map.of(prefix + "." + name + ".endpoint-override",
                                container.getEndpoint().toString(),
                                prefix + "." + name + ".region", name.equals("first") ? "us-east-1" : "eu-west-1");
                    }
                };
                var requests = List.of(new DevServicesAwsStackProviderBuildItem("test", settings, provider));
                var request = DevServicesResultBuildItem.owned().feature("client-config-reload").serviceName(prefix)
                        .serviceConfig(AbstractDevServicesAwsStackProcessor.buildServiceConfig(
                                config.getConfigMapping(GlobalDevServicesBuildTimeConfig.class),
                                config.getConfigMapping(FlociDevServicesBuildTimeConfig.class), requests))
                        .startable(() -> stack).configProvider(s -> Map.of("stack.endpoint", s.getConnectionInfo())).build();
                var additional = new FlociDevServicesProcessor().clientConfig("stack.endpoint", requests);
                Map<String, String> actual = new HashMap<>();
                registry.start(request, List.of(), List.of(additional), actual, new HashMap<>());
                assertEquals("http://localhost:4566", actual.get(prefix + "." + name + ".endpoint-override"));
                assertEquals(name.equals("first") ? "us-east-1" : "eu-west-1", actual.get(prefix + "." + name + ".region"));
                if (name.equals("second")) {
                    assertFalse(actual.containsKey(prefix + ".first.endpoint-override"));
                }
            }
            assertEquals(1, starts.get());
            assertEquals(0, closes.get());
        } finally {
            registry.closeOwnRunningServices();
        }
        assertEquals(1, closes.get());
    }
}
