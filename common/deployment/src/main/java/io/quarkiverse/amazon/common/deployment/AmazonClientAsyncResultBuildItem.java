package io.quarkiverse.amazon.common.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/*
 * Describes what async clients are provided by a given extension
 */
public final class AmazonClientAsyncResultBuildItem extends MultiBuildItem {

    private final String awsClientName;
    private String clientName;

    public AmazonClientAsyncResultBuildItem(String awsClientName, String clientName) {
        this.awsClientName = awsClientName;
        this.clientName = clientName;
    }

    public String getAwsClientName() {
        return awsClientName;
    }

    public String getClientName() {
        return clientName;
    }
}
