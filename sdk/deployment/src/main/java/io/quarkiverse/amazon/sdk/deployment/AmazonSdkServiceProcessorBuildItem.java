package io.quarkiverse.amazon.sdk.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/** Prepared build-time state shared by the setup and builder recording steps. */
final class AmazonSdkServiceProcessorBuildItem extends MultiBuildItem {
    final GenericServiceProcessor processor;

    AmazonSdkServiceProcessorBuildItem(GenericServiceProcessor processor) {
        this.processor = processor;
    }
}
