package io.quarkiverse.amazon.common.deployment.spi;

import java.util.Map;

import org.testcontainers.containers.localstack.LocalStackContainer;

/**
 * Allows to prepare a localstack container and get client properties
 */
public interface DevServicesAmazonProvider {
    /**
     * Prepare a new container owned by this application
     *
     * @param localstack The new localstack container
     * @return properties to configure a client to consume the container
     */
    Map<String, String> prepareLocalStack(LocalStackContainer localstack);

    /**
     * Prepare an existing container not owned by this application
     *
     * @param localstack The borrowed localstack container
     * @return properties to configure a client to consume the container
     */
    Map<String, String> reuseLocalStack(BorrowedLocalStackContainer localstack);
}
