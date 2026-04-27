package io.quarkiverse.amazon.devservices.cognitouserpools;

import io.quarkiverse.amazon.cognitouserpools.runtime.CognitoUserPoolsBuildTimeConfig;
import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesAwsStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class CognitoUserPoolsDevServicesAwsStackProcessor extends AbstractDevServicesAwsStackProcessor {

    @BuildStep
    DevServicesAwsStackProviderBuildItem setupCognitouserpools(CognitoUserPoolsBuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup("cognito-user-pools", clientBuildTimeConfig.devservices(), globalConfig);
    }
}
