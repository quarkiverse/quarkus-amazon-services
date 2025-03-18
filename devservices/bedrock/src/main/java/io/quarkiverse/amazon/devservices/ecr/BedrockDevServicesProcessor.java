package io.quarkiverse.amazon.devservices.ecr;

import org.testcontainers.containers.localstack.LocalStackContainer.EnabledService;

import io.quarkiverse.amazon.bedrock.BedrockBuildTimeConfig;
import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkus.deployment.annotations.BuildStep;

public class BedrockDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupEcr(BedrockBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(EnabledService.named("bedrock"), clientBuildTimeConfig.devservices());
    }
}
