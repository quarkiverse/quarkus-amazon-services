package io.quarkus.amazon.devservices.ecr;

import io.quarkus.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkus.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkus.amazon.ecr.runtime.EcrBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;
import org.testcontainers.containers.localstack.LocalStackContainer;

public class EcrDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupEcr(EcrBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(LocalStackContainer.EnabledService.named("ecr"), clientBuildTimeConfig.devservices());
    }
}