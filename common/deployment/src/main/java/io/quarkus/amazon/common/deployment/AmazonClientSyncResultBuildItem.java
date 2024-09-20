package io.quarkus.amazon.common.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/*
 * Describes what sync clients are provided by a given extension
 */
public final class AmazonClientSyncResultBuildItem extends MultiBuildItem {

    private final String awsClientName;
    private String clientName;

    public AmazonClientSyncResultBuildItem(String awsClientName, String clientName) {
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
