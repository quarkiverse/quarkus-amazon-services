package io.quarkus.amazon.devservices.cloudwatch;

import org.testcontainers.containers.localstack.LocalStackContainer.Service;

import io.quarkus.amazon.cloudwatch.runtime.CloudWatchLogsBuildTimeConfig;
import io.quarkus.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkus.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkus.deployment.annotations.BuildStep;

public class CloudWatchLogsDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupCloudWatchLogs(CloudWatchLogsBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(Service.CLOUDWATCHLOGS, clientBuildTimeConfig.devservices());
    }
}
