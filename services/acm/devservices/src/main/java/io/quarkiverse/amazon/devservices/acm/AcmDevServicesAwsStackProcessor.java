package io.quarkiverse.amazon.devservices.acm;

import io.quarkiverse.amazon.acm.runtime.AcmBuildTimeConfig;
import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesAwsStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class AcmDevServicesAwsStackProcessor extends AbstractDevServicesAwsStackProcessor {

    @BuildStep
    DevServicesAwsStackProviderBuildItem setupAcm(AcmBuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup("acm", clientBuildTimeConfig.devservices(), globalConfig);
    }
}
