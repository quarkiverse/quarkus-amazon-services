package io.quarkus.amazon.common.deployment.spi;

import java.util.Map;

import org.testcontainers.containers.localstack.LocalStackContainer;

public interface DevServicesAmazonProvider {
    Map<String, String> prepareLocalStack(LocalStackContainer localstack);

    Map<String, String> reuseLocalStack(SharedLocalStackContainer localstack);
}
