package io.quarkiverse.amazon.devservices.scheduler;

import org.testcontainers.containers.localstack.LocalStackContainer.EnabledService;

import io.quarkiverse.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkiverse.amazon.common.deployment.spi.LegacyAbstractDevServicesLocalStackProcessor;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.scheduler.runtime.SchedulerBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class SchedulerDevServicesProcessor extends LegacyAbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupScheduler(SchedulerBuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup(EnabledService.named("scheduler"), clientBuildTimeConfig.devservices(), globalConfig);
    }
}
