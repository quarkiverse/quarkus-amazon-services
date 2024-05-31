package io.quarkiverse.amazon.devservices.kms;

import org.testcontainers.containers.localstack.LocalStackContainer.Service;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkiverse.amazon.kms.runtime.KmsBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class KmsDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupKms(KmsBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(Service.KMS, clientBuildTimeConfig.devservices());
    }
}
