package io.quarkiverse.amazon.common.deployment.spi;

import java.net.URI;

/**
 * Exposes configuration of an aws container
 */
public interface AwsStackContainer {
    public URI getEndpoint();

    public String getRegion();

    public String getAccessKey();

    public String getSecretKey();
}
