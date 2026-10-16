package io.quarkiverse.amazon.devservices.scheduler;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesAwsStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.scheduler.runtime.SchedulerBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class SchedulerDevServicesAwsStackProcessor extends AbstractDevServicesAwsStackProcessor {

    @BuildStep
    DevServicesAwsStackProviderBuildItem setupScheduler(SchedulerBuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup("eventbridge-scheduler", clientBuildTimeConfig.devservices(), globalConfig);
    }
}
