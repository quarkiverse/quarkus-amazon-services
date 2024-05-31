package io.quarkiverse.amazon.devservices.eventbridge;

import org.testcontainers.containers.localstack.LocalStackContainer.EnabledService;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkiverse.amazon.eventbridge.runtime.EventBridgeBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class EventBridgeDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupEventBridge(EventBridgeBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(EnabledService.named("events"), clientBuildTimeConfig.devservices());
    }
}
