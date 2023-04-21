package io.quarkus.amazon.devservices.sts;

import org.testcontainers.containers.localstack.LocalStackContainer.Service;

import io.quarkus.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkus.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkus.amazon.sts.runtime.StsBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class StsDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupSts(StsBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(Service.STS, clientBuildTimeConfig.devservices);
    }
}