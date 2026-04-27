package io.quarkiverse.amazon.common.deployment.devservices;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesAwsStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.LocalStackDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.MiniStackDevServicesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class DevServicesAdditionalServicesAwsStackServiceProcessor extends AbstractDevServicesAwsStackProcessor {

    @BuildStep
    List<DevServicesAwsStackProviderBuildItem> setupServices(
            MiniStackDevServicesBuildTimeConfig miniStackDevServicesBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return miniStackDevServicesBuildTimeConfig.additionalServices().entrySet().stream()
                .map(entry -> setupService(entry.getKey(), entry.getValue(), globalConfig))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @BuildStep
    List<DevServicesAwsStackProviderBuildItem> setupServices(
            LocalStackDevServicesBuildTimeConfig localStackDevServicesBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return localStackDevServicesBuildTimeConfig.additionalServices().entrySet().stream()
                .map(entry -> setupService(entry.getKey(), entry.getValue(), globalConfig))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    DevServicesAwsStackProviderBuildItem setupService(String serviceName, DevServicesBuildTimeConfig config,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return setup(serviceName, config, globalConfig);
    }
}
