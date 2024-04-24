package io.quarkus.amazon.devservices.inspector;

import org.testcontainers.containers.localstack.LocalStackContainer.Service;

import io.quarkus.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkus.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkus.amazon.inspector.runtime.InspectorBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class InspectorDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    //TODO LocalSTack Inspector service???
    @BuildStep
    DevServicesLocalStackProviderBuildItem setupInspector(InspectorBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(Service.SSM, clientBuildTimeConfig.devservices());
    }
}
