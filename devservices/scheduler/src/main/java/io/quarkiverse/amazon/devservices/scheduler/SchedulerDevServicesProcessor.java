package io.quarkiverse.amazon.devservices.scheduler;

import org.testcontainers.containers.localstack.LocalStackContainer.EnabledService;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkiverse.amazon.scheduler.runtime.SchedulerBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class SchedulerDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupEventBridge(SchedulerBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(EnabledService.named("scheduler"), clientBuildTimeConfig.devservices());
    }
}
