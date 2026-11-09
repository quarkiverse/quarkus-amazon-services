package io.quarkiverse.amazon.s3.deployment;

import io.quarkus.builder.item.MultiBuildItem;

final class S3TransferManagerBuildItem extends MultiBuildItem {
    final String clientName;
    final boolean crt;

    S3TransferManagerBuildItem(String clientName, boolean crt) {
        this.clientName = clientName;
        this.crt = crt;
    }
}
