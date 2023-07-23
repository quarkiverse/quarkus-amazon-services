package io.quarkus.amazon.devservices.dynamodb;

import org.testcontainers.containers.localstack.LocalStackContainer.Service;

import io.quarkus.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkus.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkus.amazon.dynamodb.runtime.DynamodbBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;

public class DynamodbDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupDynamodb(DynamodbBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(Service.DYNAMODB, clientBuildTimeConfig.devservices());
    }
}
