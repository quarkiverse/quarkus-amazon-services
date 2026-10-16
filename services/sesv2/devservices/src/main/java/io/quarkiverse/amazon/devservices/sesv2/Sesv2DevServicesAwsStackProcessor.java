package io.quarkiverse.amazon.devservices.sesv2;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesAwsStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.sesv2.runtime.SesV2BuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class Sesv2DevServicesAwsStackProcessor extends AbstractDevServicesAwsStackProcessor {

    @BuildStep
    DevServicesAwsStackProviderBuildItem setupSesv2(SesV2BuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup("sesv2", clientBuildTimeConfig.devservices(), globalConfig);
    }
}
