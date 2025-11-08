package io.quarkiverse.amazon.dynamodb.enhanced.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

public final class DynamoDbEnhancedTableBuildItem extends MultiBuildItem {

    private String tableName;
    private DotName beanClassName;
    private DotName clientClassName;
    private DotName tableClassName;

    public DynamoDbEnhancedTableBuildItem(String tableName, DotName beanClassName, DotName clientClassName,
            DotName tableClassName) {
        this.tableName = tableName;
        this.beanClassName = beanClassName;
        this.clientClassName = clientClassName;
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

    public DotName getTableClassName() {
        return tableClassName;
    }
}
