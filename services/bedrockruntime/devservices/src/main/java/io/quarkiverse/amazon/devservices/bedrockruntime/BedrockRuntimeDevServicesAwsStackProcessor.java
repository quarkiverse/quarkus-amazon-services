package io.quarkiverse.amazon.devservices.bedrockruntime;

import io.quarkiverse.amazon.bedrockruntime.runtime.BedrockRuntimeBuildTimeConfig;
import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesAwsStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class BedrockRuntimeDevServicesAwsStackProcessor extends AbstractDevServicesAwsStackProcessor {

    @BuildStep
    DevServicesAwsStackProviderBuildItem setupBedrockRuntime(BedrockRuntimeBuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup("bedrockruntime", clientBuildTimeConfig.devservices(), globalConfig);
    }
}