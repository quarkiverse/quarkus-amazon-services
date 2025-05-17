package io.quarkiverse.amazon.devservices.rds;

import org.testcontainers.containers.localstack.LocalStackContainer.EnabledService;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkiverse.amazon.rds.runtime.RdsBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class RdsDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupRds(RdsBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(EnabledService.named("rds"), clientBuildTimeConfig.devservices());
    }
}
