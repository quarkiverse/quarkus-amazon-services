package io.quarkiverse.amazon.dynamodb.enhanced.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

public final class DynamoDbEnhancedBeanBuildItem extends MultiBuildItem {

    private DotName className;

    public DynamoDbEnhancedBeanBuildItem(DotName className) {
        this.className = className;
    }

    public DotName getClassName() {
        return className;
    }
}
