package io.quarkiverse.amazon.dynamodb.enhanced.deployment;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.amazon.dynamodb.enhanced.runtime.NamedDynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

@ApplicationScoped
public class DynamoDBEnhancedDbTableConstructorInjection {

    private DynamoDbTable<DynamoDBExampleTableEntry> syncTable;

    DynamoDBEnhancedDbTableConstructorInjection(
            @NamedDynamoDbTable("sync") DynamoDbTable<DynamoDBExampleTableEntry> syncTable) {
        this.syncTable = syncTable;
    }

    public DynamoDbTable<DynamoDBExampleTableEntry> getSyncTable() {
        return syncTable;
    }
}
