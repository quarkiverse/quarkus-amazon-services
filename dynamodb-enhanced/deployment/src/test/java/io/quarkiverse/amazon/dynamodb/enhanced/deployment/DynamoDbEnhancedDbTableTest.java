package io.quarkiverse.amazon.dynamodb.enhanced.deployment;

import static org.junit.Assert.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.dynamodb.enhanced.runtime.NamedDynamoDbTable;
import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

public class DynamoDbEnhancedDbTableTest {

    @NamedDynamoDbTable("sync")
    @Inject
    DynamoDbTable<DynamoDBExampleTableEntry> syncTable;

    @NamedDynamoDbTable("sync")
    @Inject
    DynamoDbTable<DynamoDBExampleTableEntry> syncTableDuplicate;

    @NamedDynamoDbTable("sync-other")
    @Inject
    DynamoDbTable<DynamoDBExampleTableEntry> syncTableOther;

    @NamedDynamoDbTable("async")
    @Inject
    DynamoDbAsyncTable<DynamoDBExampleTableEntry> asyncTable;

    @NamedDynamoDbTable("async")
    @Inject
    DynamoDbAsyncTable<DynamoDBExampleTableEntry> asyncTableDuplicate;

    @NamedDynamoDbTable("async-other")
    @Inject
    DynamoDbAsyncTable<DynamoDBExampleTableEntry> asyncTableOther;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DynamoDBExampleTableEntry.class)
                    .addAsResource("full-config.properties", "application.properties"));

    @Test
    public void test() {
        // should finish with success
        assertEquals("sync", syncTable.tableName());
        assertEquals("sync", syncTableDuplicate.tableName());
        assertEquals("sync-other", syncTableOther.tableName());
        assertEquals("async", asyncTable.tableName());
        assertEquals("async", asyncTableDuplicate.tableName());
        assertEquals("async-other", asyncTableOther.tableName());
    }
}
