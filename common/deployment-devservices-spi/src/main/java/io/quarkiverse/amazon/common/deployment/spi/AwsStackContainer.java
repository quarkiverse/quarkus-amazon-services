package io.quarkiverse.amazon.common.deployment.spi;

import java.net.URI;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.regions.Region;

/**
 * Exposes configuration of an aws container
 */
public interface AwsStackContainer {
    public URI getEndpoint();

    public String getRegion();

    public String getAccessKey();

    public String getSecretKey();

    /**
     * Points the client builder at this container: endpoint, region and static credentials.
     */
    default <B extends AwsClientBuilder<B, ?>> B configure(B builder) {
        return builder
                .endpointOverride(getEndpoint())
                .region(Region.of(getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials
                        .create(getAccessKey(), getSecretKey())));
    }
}
