package io.quarkiverse.amazon.devservices.cloudwatch;

import io.quarkiverse.amazon.cloudwatchlogs.runtime.CloudWatchLogsBuildTimeConfig;
import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesAwsStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class CloudWatchLogsDevServicesAwsStackProcessor extends AbstractDevServicesAwsStackProcessor {

    @BuildStep
    DevServicesAwsStackProviderBuildItem setupCloudwatchlogs(CloudWatchLogsBuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup("cloudwatchlogs", clientBuildTimeConfig.devservices(), globalConfig);
    }
}
