package io.quarkiverse.amazon.devservices.sfn;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesAwsStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.sfn.runtime.SfnBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class SfnDevServicesAwsStackProcessor extends AbstractDevServicesAwsStackProcessor {

    @BuildStep
    DevServicesAwsStackProviderBuildItem setupSfn(SfnBuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup("sfn", clientBuildTimeConfig.devservices(), globalConfig);
    }
}
