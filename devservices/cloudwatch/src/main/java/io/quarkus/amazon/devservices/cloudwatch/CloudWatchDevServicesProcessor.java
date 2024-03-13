package io.quarkus.amazon.devservices.cloudwatch;

import org.testcontainers.containers.localstack.LocalStackContainer.Service;

import io.quarkus.amazon.cloudwatch.runtime.CloudWatchBuildTimeConfig;
import io.quarkus.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkus.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkus.deployment.annotations.BuildStep;

public class CloudWatchDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupCloudWatch(CloudWatchBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(Service.CLOUDWATCH, clientBuildTimeConfig.devservices());
    }
}
