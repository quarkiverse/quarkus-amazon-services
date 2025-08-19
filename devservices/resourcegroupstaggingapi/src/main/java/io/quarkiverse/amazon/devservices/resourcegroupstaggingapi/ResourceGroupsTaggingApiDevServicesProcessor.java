package io.quarkiverse.amazon.devservices.resourcegroupstaggingapi;

import org.testcontainers.containers.localstack.LocalStackContainer.EnabledService;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkiverse.amazon.resourcegroupstaggingapi.runtime.ResourceGroupsTaggingApiBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class ResourceGroupsTaggingApiDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupSsm(ResourceGroupsTaggingApiBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(EnabledService.named("resourcegroupstaggingapi"), clientBuildTimeConfig.devservices());
    }
}
