package io.quarkiverse.amazon.devservices.myecr;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesAwsStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.myecr.runtime.MyEcrBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;
import software.amazon.awssdk.annotations.Generated;

@Generated("io.quarkiverse.amazon:codegen")
public class MyEcrDevServicesAwsStackProcessor extends AbstractDevServicesAwsStackProcessor {
    @BuildStep
    DevServicesAwsStackProviderBuildItem setupMyEcr(MyEcrBuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup("my-ecr", clientBuildTimeConfig.devservices(), globalConfig);
    }
}
