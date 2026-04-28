package io.quarkiverse.amazon.devservices.apigateway;

import org.testcontainers.containers.localstack.LocalStackContainer.EnabledService;

import io.quarkiverse.amazon.apigateway.runtime.ApiGatewayBuildTimeConfig;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkiverse.amazon.common.deployment.spi.LegacyAbstractDevServicesLocalStackProcessor;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class ApiGatewayDevServicesProcessor extends LegacyAbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupApiGateway(ApiGatewayBuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup(EnabledService.named("apigateway"), clientBuildTimeConfig.devservices(), globalConfig);
    }
}
