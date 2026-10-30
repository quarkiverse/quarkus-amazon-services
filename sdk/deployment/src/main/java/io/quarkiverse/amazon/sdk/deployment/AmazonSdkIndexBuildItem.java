package io.quarkiverse.amazon.sdk.deployment;

import org.jboss.jandex.IndexView;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Index of the AWS SDK service classes, kept out of the combined index so that SDK internals are neither
 * scanned by ArC nor picked up by index-wide lookups such as the interceptor discovery.
 */
final class AmazonSdkIndexBuildItem extends SimpleBuildItem {
    final IndexView index;

    AmazonSdkIndexBuildItem(IndexView index) {
        this.index = index;
    }
}
