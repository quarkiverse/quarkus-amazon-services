package io.quarkiverse.amazon.devservices.sns;

import org.testcontainers.containers.localstack.LocalStackContainer.Service;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkiverse.amazon.sns.runtime.SnsBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class SnsDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupSns(SnsBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(Service.SNS, clientBuildTimeConfig.devservices());
    }
}
