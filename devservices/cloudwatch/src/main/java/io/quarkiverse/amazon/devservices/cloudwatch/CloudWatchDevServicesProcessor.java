package io.quarkiverse.amazon.devservices.cloudwatch;

import org.testcontainers.containers.localstack.LocalStackContainer.Service;

import io.quarkiverse.amazon.cloudwatch.runtime.CloudWatchBuildTimeConfig;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkiverse.amazon.common.deployment.spi.LegacyAbstractDevServicesLocalStackProcessor;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class CloudWatchDevServicesProcessor extends LegacyAbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupCloudWatch(CloudWatchBuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup(Service.CLOUDWATCH, clientBuildTimeConfig.devservices(), globalConfig);
    }
}
