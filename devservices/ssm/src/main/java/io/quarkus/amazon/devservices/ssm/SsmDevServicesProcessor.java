package io.quarkus.amazon.devservices.ssm;

import org.testcontainers.containers.localstack.LocalStackContainer.Service;

import io.quarkus.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkus.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkus.amazon.ssm.runtime.SsmBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class SsmDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupSsm(SsmBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(Service.SSM, clientBuildTimeConfig.devservices);
    }
}
