package io.quarkiverse.amazon.dynamodb.enhanced.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

public final class DynamodbEnhancedBeanBuildItem extends MultiBuildItem {

    private DotName className;

    public DynamodbEnhancedBeanBuildItem(DotName className) {
        this.className = className;
    }

    public DotName getClassName() {
        return className;
    }
}
