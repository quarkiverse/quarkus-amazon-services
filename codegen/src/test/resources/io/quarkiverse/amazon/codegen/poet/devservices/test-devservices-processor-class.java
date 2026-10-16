package io.quarkiverse.amazon.devservices.ecr;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesAwsStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.ecr.runtime.EcrBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;
import software.amazon.awssdk.annotations.Generated;

@Generated("io.quarkiverse.amazon:codegen")
public class EcrDevServicesAwsStackProcessor extends AbstractDevServicesAwsStackProcessor {
    @BuildStep
    DevServicesAwsStackProviderBuildItem setupEcr(EcrBuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup("ecr", clientBuildTimeConfig.devservices(), globalConfig);
    }
}
