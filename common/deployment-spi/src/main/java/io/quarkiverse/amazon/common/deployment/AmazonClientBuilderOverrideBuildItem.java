package io.quarkiverse.amazon.common.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class AmazonClientBuilderOverrideBuildItem extends MultiBuildItem {
    private final Class<?> builderClass;
    private final String clientName;

    public AmazonClientBuilderOverrideBuildItem(Class<?> builderClass,
            String clientName) {
        this.builderClass = builderClass;
        this.clientName = clientName;
    }

    public Class<?> getBuilderClass() {
        return builderClass;
    }

    public String getClientName() {
        return clientName;
    }

}
