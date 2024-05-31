package io.quarkiverse.amazon.devservices.dynamodb;

import org.testcontainers.containers.localstack.LocalStackContainer.Service;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkiverse.amazon.dynamodb.runtime.DynamodbBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class DynamodbDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupDynamodb(DynamodbBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(Service.DYNAMODB, clientBuildTimeConfig.devservices());
    }
}
