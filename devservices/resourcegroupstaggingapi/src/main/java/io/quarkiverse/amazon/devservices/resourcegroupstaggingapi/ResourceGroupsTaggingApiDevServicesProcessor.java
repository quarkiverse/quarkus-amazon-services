package io.quarkiverse.amazon.devservices.resourcegroupstaggingapi;

import org.testcontainers.containers.localstack.LocalStackContainer.EnabledService;

import io.quarkiverse.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkiverse.amazon.common.deployment.spi.LegacyAbstractDevServicesLocalStackProcessor;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.resourcegroupstaggingapi.runtime.ResourceGroupsTaggingApiBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class ResourceGroupsTaggingApiDevServicesProcessor extends LegacyAbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupResourceGroupsTaggingApi(
            ResourceGroupsTaggingApiBuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup(EnabledService.named("resourcegroupstaggingapi"), clientBuildTimeConfig.devservices(), globalConfig);
    }
}
