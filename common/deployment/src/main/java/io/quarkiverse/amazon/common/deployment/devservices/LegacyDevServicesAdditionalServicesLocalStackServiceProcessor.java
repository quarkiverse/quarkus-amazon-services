package io.quarkiverse.amazon.common.deployment.devservices;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.testcontainers.containers.localstack.LocalStackContainer.EnabledService;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.LocalStackDevServicesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class LegacyDevServicesAdditionalServicesLocalStackServiceProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    List<DevServicesLocalStackProviderBuildItem> setupServices(
            LocalStackDevServicesBuildTimeConfig localStackDevServicesBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return localStackDevServicesBuildTimeConfig.additionalServices().entrySet().stream()
                .map(entry -> setupService(entry.getKey(), entry.getValue(), globalConfig))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    DevServicesLocalStackProviderBuildItem setupService(String serviceName, DevServicesBuildTimeConfig config,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return setup(EnabledService.named(serviceName), config, globalConfig);
    }
}
