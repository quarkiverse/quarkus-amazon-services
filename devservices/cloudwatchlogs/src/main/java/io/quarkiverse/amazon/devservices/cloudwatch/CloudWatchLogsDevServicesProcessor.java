package io.quarkiverse.amazon.devservices.cloudwatch;

import org.testcontainers.containers.localstack.LocalStackContainer.Service;

import io.quarkiverse.amazon.cloudwatch.runtime.CloudWatchLogsBuildTimeConfig;
import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkus.deployment.annotations.BuildStep;

public class CloudWatchLogsDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupCloudWatchLogs(CloudWatchLogsBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(Service.CLOUDWATCHLOGS, clientBuildTimeConfig.devservices());
    }
}
