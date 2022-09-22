package io.quarkus.amazon.common.deployment.spi;

import java.net.URI;

import org.testcontainers.containers.localstack.LocalStackContainer.EnabledService;

/**
 * Exposes configuration of an existing localstack container borrowed from another application
 */
public interface BorrowedLocalStackContainer {
    public URI getEndpointOverride(EnabledService enabledService);

    public String getRegion();

    public String getAccessKey();

    public String getSecretKey();
}
