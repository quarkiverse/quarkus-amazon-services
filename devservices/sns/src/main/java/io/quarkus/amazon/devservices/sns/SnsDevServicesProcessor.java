package io.quarkus.amazon.devservices.sns;

import org.testcontainers.containers.localstack.LocalStackContainer.Service;

import io.quarkus.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkus.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkus.amazon.sns.runtime.SnsBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class SnsDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupSns(SnsBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(Service.SNS, clientBuildTimeConfig.devservices());
    }
}
