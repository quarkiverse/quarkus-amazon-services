package io.quarkus.amazon.secretsmanager.config.deployment;

import java.util.Optional;

import io.quarkus.amazon.common.runtime.AbstractAmazonClientTransportRecorder;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;

public final class SecretsManagerConfigTransportBuildItem extends MultiBuildItem {

    private Optional<RuntimeValue<AbstractAmazonClientTransportRecorder>> syncTransporter;
    private Optional<RuntimeValue<AbstractAmazonClientTransportRecorder>> asyncTransporter;

    public SecretsManagerConfigTransportBuildItem(Optional<RuntimeValue<AbstractAmazonClientTransportRecorder>> syncTransporter,
            Optional<RuntimeValue<AbstractAmazonClientTransportRecorder>> asyncTransporter) {
        this.syncTransporter = syncTransporter;
        this.asyncTransporter = asyncTransporter;
    }

    public Optional<RuntimeValue<AbstractAmazonClientTransportRecorder>> getSyncTransporter() {
        return syncTransporter;
    }

    public Optional<RuntimeValue<AbstractAmazonClientTransportRecorder>> getAsyncTransporter() {
        return asyncTransporter;
    }
}
