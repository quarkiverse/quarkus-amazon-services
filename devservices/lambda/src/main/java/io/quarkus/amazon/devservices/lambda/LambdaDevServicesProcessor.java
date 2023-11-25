package io.quarkus.amazon.devservices.lambda;

import org.testcontainers.containers.localstack.LocalStackContainer.Service;

import io.quarkus.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkus.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkus.amazon.common.deployment.spi.LocalStackDevServicesBaseConfig;
import io.quarkus.amazon.lambda.runtime.LambdaBuildTimeConfig;
import io.quarkus.amazon.lambda.runtime.LambdaDevServicesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class LambdaDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    @SuppressWarnings("unused")
    DevServicesLocalStackProviderBuildItem setupLambda(LambdaBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(Service.LAMBDA, clientBuildTimeConfig.devservices());
    }

    private static final class LambdaDevServiceCfg extends LocalStackDevServicesBaseConfig {

        public LambdaDevServiceCfg(LambdaDevServicesBuildTimeConfig config) {
            super(config.shared(), config.serviceName(), config.containerProperties());
        }
    }
}
