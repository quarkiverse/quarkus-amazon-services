package io.quarkiverse.amazon.common.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;

public final class AmazonClientBuilderBuildItem extends MultiBuildItem {
    private final RuntimeValue<AwsClientBuilder> clientBuilder;
    private final Class<?> builderClass;
    private final String clientName;
    private final boolean hasOpenTelemetry;

    public AmazonClientBuilderBuildItem(RuntimeValue<AwsClientBuilder> clientBuilder, Class<?> builderClass,
            String clientName, boolean hasOpenTelemetry) {
        this.clientBuilder = clientBuilder;
        this.builderClass = builderClass;
        this.clientName = clientName;
        this.hasOpenTelemetry = hasOpenTelemetry;
    }

    public RuntimeValue<AwsClientBuilder> getClientBuilder() {
        return clientBuilder;
    }

    public Class<?> getBuilderClass() {
        return builderClass;
    }

    public String getClientName() {
        return clientName;
    }

    public boolean hasOpenTelemetry() {
        return hasOpenTelemetry;
    }
}
