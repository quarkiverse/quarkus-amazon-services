package io.quarkiverse.amazon.dynamodb.enhanced.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.gizmo.MethodDescriptor;

public final class DynamodbEnhancedTableBuildItem extends MultiBuildItem {

    private String tableName;
    private DotName beanClassName;
    private DotName clientClassName;
    private MethodDescriptor tableMethodDescriptor;
    private DotName tableClassName;

    public DynamodbEnhancedTableBuildItem(String tableName, DotName beanClassName, DotName clientClassName,
            MethodDescriptor tableMethodDescriptor,
            DotName tableClassName) {
        this.tableName = tableName;
        this.beanClassName = beanClassName;
        this.clientClassName = clientClassName;
        this.tableMethodDescriptor = tableMethodDescriptor;
        this.tableClassName = tableClassName;
    }

    public String getTableName() {
        return tableName;
    }

    public DotName getBeanClassName() {
        return beanClassName;
    }

    public DotName getClientClassName() {
        return clientClassName;
    }

    public MethodDescriptor getTableMethodDescriptor() {
        return tableMethodDescriptor;
    }

    public DotName getTableClassName() {
        return tableClassName;
    }
}
