package io.quarkiverse.amazon.devservices.lambda;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesAwsStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.lambda.runtime.LambdaBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class LambdaDevServicesAwsStackProcessor extends AbstractDevServicesAwsStackProcessor {

    @BuildStep
    DevServicesAwsStackProviderBuildItem setupLambda(LambdaBuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup("lambda", clientBuildTimeConfig.devservices(), globalConfig);
    }
}
