package io.quarkiverse.amazon.devservices.apigateway;

import io.quarkiverse.amazon.apigateway.runtime.ApiGatewayBuildTimeConfig;
import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesAwsStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class ApiGatewayDevServicesAwsStackProcessor extends AbstractDevServicesAwsStackProcessor {

    @BuildStep
    DevServicesAwsStackProviderBuildItem setupApigateway(ApiGatewayBuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup("apigateway", clientBuildTimeConfig.devservices(), globalConfig);
    }
}
