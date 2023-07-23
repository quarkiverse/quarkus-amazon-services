package io.quarkus.amazon.devservices.ses;

import org.testcontainers.containers.localstack.LocalStackContainer.Service;

import io.quarkus.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkus.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkus.amazon.ses.runtime.SesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class SesDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupSes(SesBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(Service.SES, clientBuildTimeConfig.devservices());
    }
}
