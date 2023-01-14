package io.quarkus.amazon.common.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/*
 * Describes what async clients are provided by a given extension
 */
public final class AmazonClientAsyncResultBuildItem extends MultiBuildItem {

    private final String awsClientName;

    public AmazonClientAsyncResultBuildItem(String awsClientName) {
        this.awsClientName = awsClientName;
    }

    public String getAwsClientName() {
        return awsClientName;
    }
}
