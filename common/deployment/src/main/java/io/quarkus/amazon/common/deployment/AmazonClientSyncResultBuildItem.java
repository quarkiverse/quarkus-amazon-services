package io.quarkus.amazon.common.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/*
 * Describes what sync clients are provided by a given extension
 */
public final class AmazonClientSyncResultBuildItem extends MultiBuildItem {

    private final String awsClientName;

    public AmazonClientSyncResultBuildItem(String awsClientName) {
        this.awsClientName = awsClientName;
    }

    public String getAwsClientName() {
        return awsClientName;
    }
}
