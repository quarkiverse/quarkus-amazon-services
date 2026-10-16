package io.quarkiverse.amazon.devservices.secretsmanager;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesAwsStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.secretsmanager.runtime.SecretsManagerBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class SecretsManagerDevServicesAwsStackProcessor extends AbstractDevServicesAwsStackProcessor {

    @BuildStep
    DevServicesAwsStackProviderBuildItem setupSecretsmanager(SecretsManagerBuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup("secretsmanager", clientBuildTimeConfig.devservices(), globalConfig);
    }
}
