package io.quarkiverse.amazon.devservices.eventbridge;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesAwsStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.eventbridge.runtime.EventBridgeBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class EventBridgeDevServicesAwsStackProcessor extends AbstractDevServicesAwsStackProcessor {

    @BuildStep
    DevServicesAwsStackProviderBuildItem setupEventbridge(EventBridgeBuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup("eventbridge", clientBuildTimeConfig.devservices(), globalConfig);
    }
}
