package io.quarkiverse.amazon.common.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * Represents a build item that holds the runtime value of an Amazon client builder instance.
 * For internal use only
 */
public final class AmazonClientExtensionBuilderInstanceBuildItem extends MultiBuildItem {

    private final String clientName;
    private final Class<?> clientBuilderClass;
    private RuntimeValue<?> builderValue;

    public AmazonClientExtensionBuilderInstanceBuildItem(
            String clientName,
            Class<?> clientBuilderClass,
            RuntimeValue<?> builderValue) {
        this.clientName = clientName;
        this.clientBuilderClass = clientBuilderClass;
        this.builderValue = builderValue;
    }

    public String getClientName() {
        return clientName;
    }

    public Class<?> getClientBuilderClass() {
        return clientBuilderClass;
    }

    public RuntimeValue<?> getBuilder() {
        return builderValue;
    }
}
