package io.quarkus.amazon.devservices.sqs;

import org.testcontainers.containers.localstack.LocalStackContainer.Service;

import io.quarkus.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkus.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkus.amazon.sqs.runtime.SqsBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class SqsDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupSqs(SqsBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(Service.SQS, clientBuildTimeConfig.devservices);
    }
}
