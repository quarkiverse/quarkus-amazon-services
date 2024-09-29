package io.quarkiverse.amazon.devservices.sfn;

import org.testcontainers.containers.localstack.LocalStackContainer.Service;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkiverse.amazon.sfn.runtime.SfnBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class SfnDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupSfn(SfnBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(Service.STEPFUNCTIONS, clientBuildTimeConfig.devservices());
    }
}
