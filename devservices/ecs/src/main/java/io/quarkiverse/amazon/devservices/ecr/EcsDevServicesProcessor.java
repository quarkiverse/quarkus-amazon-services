package io.quarkiverse.amazon.devservices.ecs;

import org.testcontainers.containers.localstack.LocalStackContainer.EnabledService;

import io.quarkiverse.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkiverse.amazon.common.deployment.spi.LegacyAbstractDevServicesLocalStackProcessor;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.ecs.runtime.EcsBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class EcsDevServicesProcessor extends LegacyAbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupEcs(EcsBuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup(EnabledService.named("ecs"), clientBuildTimeConfig.devservices(), globalConfig);
    }
}
