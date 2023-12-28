package io.quarkus.amazon.devservices.sfn;

import org.testcontainers.containers.localstack.LocalStackContainer.Service;

import io.quarkus.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkus.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkus.amazon.sfn.runtime.SfnBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class SfnDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupSfn(SfnBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(Service.STEPFUNCTIONS, clientBuildTimeConfig.devservices());
    }
}
