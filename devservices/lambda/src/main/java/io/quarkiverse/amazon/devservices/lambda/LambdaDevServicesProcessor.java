package io.quarkiverse.amazon.devservices.lambda;

import org.testcontainers.containers.localstack.LocalStackContainer.Service;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkiverse.amazon.lambda.runtime.LambdaBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class LambdaDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupLambda(LambdaBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(Service.LAMBDA, clientBuildTimeConfig.devservices());
    }
}
