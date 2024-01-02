package io.quarkus.amazon.devservices.kinesis;

import org.testcontainers.containers.localstack.LocalStackContainer.Service;

import io.quarkus.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkus.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkus.amazon.kinesis.runtime.KinesisBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class KinesisDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupKinesis(KinesisBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(Service.KINESIS, clientBuildTimeConfig.devservices());
    }
}
