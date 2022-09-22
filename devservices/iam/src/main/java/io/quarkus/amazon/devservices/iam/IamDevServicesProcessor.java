package io.quarkus.amazon.devservices.iam;

import org.testcontainers.containers.localstack.LocalStackContainer.Service;

import io.quarkus.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkus.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkus.amazon.iam.runtime.IamBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class IamDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupIam(IamBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(Service.IAM, clientBuildTimeConfig.devservices);
    }
}
