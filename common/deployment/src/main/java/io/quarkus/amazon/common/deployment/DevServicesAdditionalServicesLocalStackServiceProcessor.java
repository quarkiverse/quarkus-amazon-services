package io.quarkus.amazon.common.deployment;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.testcontainers.containers.localstack.LocalStackContainer.EnabledService;

import io.quarkus.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkus.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkus.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkus.amazon.common.runtime.LocalStackDevServicesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class DevServicesAdditionalServicesLocalStackServiceProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    List<DevServicesLocalStackProviderBuildItem> setupServices(
            LocalStackDevServicesBuildTimeConfig localStackDevServicesBuildTimeConfig) {
        return localStackDevServicesBuildTimeConfig.additionalServices.entrySet().stream()
                .map(entry -> setupService(entry.getKey(), entry.getValue()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    DevServicesLocalStackProviderBuildItem setupService(String serviceName, DevServicesBuildTimeConfig config) {
        return setup(EnabledService.named(serviceName), config);
    }
}