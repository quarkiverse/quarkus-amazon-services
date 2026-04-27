package io.quarkiverse.amazon.devservices.cloudwatch;

import io.quarkiverse.amazon.cloudwatch.runtime.CloudWatchBuildTimeConfig;
import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesAwsStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class CloudWatchDevServicesAwsStackProcessor extends AbstractDevServicesAwsStackProcessor {

    @BuildStep
    DevServicesAwsStackProviderBuildItem setupCloudwatch(CloudWatchBuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup("cloudwatch", clientBuildTimeConfig.devservices(), globalConfig);
    }
}