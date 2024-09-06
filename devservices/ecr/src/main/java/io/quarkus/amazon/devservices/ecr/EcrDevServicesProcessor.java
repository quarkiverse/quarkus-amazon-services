package io.quarkus.amazon.devservices.ecr;

import org.testcontainers.containers.localstack.LocalStackContainer.EnabledService;

import io.quarkus.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkus.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkus.amazon.ecr.runtime.EcrBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class EcrDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupEcr(EcrBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(EnabledService.named("ecr"), clientBuildTimeConfig.devservices());
    }
}
