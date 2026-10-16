package io.quarkiverse.amazon.devservices.resourcegroupstaggingapi;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesAwsStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.resourcegroupstaggingapi.runtime.ResourceGroupsTaggingApiBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class ResourceGroupsTaggingApiDevServicesAwsStackProcessor extends AbstractDevServicesAwsStackProcessor {

    @BuildStep
    DevServicesAwsStackProviderBuildItem setupResourcegroupstaggingapi(
            ResourceGroupsTaggingApiBuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup("resourcegroupstaggingapi", clientBuildTimeConfig.devservices(), globalConfig);
    }
}