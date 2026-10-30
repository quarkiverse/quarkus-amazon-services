package io.quarkiverse.amazon.sdk.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

final class AmazonSdkServiceBuildItem extends MultiBuildItem {
    final String service;
    final DotName syncClient;
    final DotName asyncClient;
    final DotName syncBuilder;
    final DotName asyncBuilder;

    AmazonSdkServiceBuildItem(String service, DotName syncClient, DotName asyncClient, DotName syncBuilder,
            DotName asyncBuilder) {
        this.service = service;
        this.syncClient = syncClient;
        this.asyncClient = asyncClient;
        this.syncBuilder = syncBuilder;
        this.asyncBuilder = asyncBuilder;
    }
}
