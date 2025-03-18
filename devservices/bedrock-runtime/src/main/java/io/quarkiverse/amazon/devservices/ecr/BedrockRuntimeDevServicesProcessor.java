package io.quarkiverse.amazon.devservices.ecr;

import org.testcontainers.containers.localstack.LocalStackContainer.EnabledService;

import io.quarkiverse.amazon.bedrockruntime.runtime.BedrockRuntimeBuildTimeConfig;
import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkus.deployment.annotations.BuildStep;

public class BedrockRuntimeDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupEcr(BedrockRuntimeBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(EnabledService.named("bedrock"), clientBuildTimeConfig.devservices());
    }

    @Override
    protected String getPropertyConfigurationName(EnabledService enabledService) {
        return "bedrock-runtime";
    }
}
