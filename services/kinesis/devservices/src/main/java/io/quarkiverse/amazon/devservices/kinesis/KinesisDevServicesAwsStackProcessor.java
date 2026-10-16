package io.quarkiverse.amazon.devservices.kinesis;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesAwsStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.kinesis.runtime.KinesisBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class KinesisDevServicesAwsStackProcessor extends AbstractDevServicesAwsStackProcessor {

    @BuildStep
    DevServicesAwsStackProviderBuildItem setupKinesis(KinesisBuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup("kinesis", clientBuildTimeConfig.devservices(), globalConfig);
    }
}
