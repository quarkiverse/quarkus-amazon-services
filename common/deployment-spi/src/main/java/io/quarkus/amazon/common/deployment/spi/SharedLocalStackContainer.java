package io.quarkus.amazon.common.deployment.spi;

import java.net.URI;

import org.testcontainers.containers.localstack.LocalStackContainer.EnabledService;

public interface SharedLocalStackContainer {
    public URI getEndpointOverride(EnabledService enabledService);

    public String getRegion();

    public String getAccessKey();

    public String getSecretKey();
}
