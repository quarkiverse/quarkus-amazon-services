package io.quarkus.amazon.devservices.cognitouserpools;

import org.testcontainers.containers.localstack.LocalStackContainer;

import io.quarkus.amazon.cognitouserpools.runtime.CognitoUserPoolsBuildTimeConfig;
import io.quarkus.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkus.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkus.deployment.annotations.BuildStep;

public class CognitoUserPoolsDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupCognitoUserPools(CognitoUserPoolsBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(LocalStackContainer.EnabledService.named("cognito-user-pools"), clientBuildTimeConfig.devservices);
    }
}
