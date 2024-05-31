package io.quarkiverse.amazon.devservices.secretsmanager;

import org.testcontainers.containers.localstack.LocalStackContainer.Service;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkiverse.amazon.secretsmanager.runtime.SecretsManagerBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class SecretsManagerDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupSecretsManager(SecretsManagerBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(Service.SECRETSMANAGER, clientBuildTimeConfig.devservices());
    }
}
