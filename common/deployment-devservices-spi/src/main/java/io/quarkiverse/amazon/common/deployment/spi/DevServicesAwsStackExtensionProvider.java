package io.quarkiverse.amazon.common.deployment.spi;

import java.util.Map;
import java.util.function.Function;

/**
 * Provider interface for configuring stack containers and getting client properties.
 * <p>
 * Allows service-specific customization of stack container configuration and
 * AWS SDK client properties.
 * </p>
 */
public interface DevServicesAwsStackExtensionProvider {

    /**
     * Prepare a new stack container owned by this application.
     * <p>
     * This method is called before the container is started, allowing service-specific
     * initialization (e.g., creating S3 buckets, DynamoDB tables, etc.).
     * </p>
     *
     * @param awsStack The new stack container
     * @return properties to configure the AWS SDK client to consume the container
     */
    void prepareAwsStackContainer(AwsStackContainer awsStack);

    /**
     * Reuse an existing Ministack container not owned by this application.
     * <p>
     * This method is called when a Ministack container is being reused across multiple
     * applications or tests.
     * </p>
     *
     * @param awsStack The borrowed stack container
     * @return properties to configure the AWS SDK client to consume the container
     */
    void reuseAwsStackContainer(AwsStackContainer awsStack);

    /**
     * Get client configuration properties for a Ministack container.
     * <p>
     * This method retrieves the AWS SDK client configuration for a running container
     * without any side effects. It is used by the dev services config provider to
     * dynamically supply configuration at runtime.
     * </p>
     *
     * @return properties to configure the AWS SDK client
     */
    Map<String, Function<AwsStackContainer, String>> getClientConfig();
}
