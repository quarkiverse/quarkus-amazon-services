package io.quarkiverse.it.amazon.dynamodb.enhanced;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

import org.jboss.logging.Logger;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

public class DynamoDbEnhancedAbstractResource {

    private static final Logger LOG = Logger.getLogger(DynamoDbEnhancedAbstractResource.class);

    private final static String PAYLOAD_VALUE = "OK";

    protected CompletionStage<String> testAsyncDynamo(DynamoDbAsyncTable<DynamoDBExampleTableEntry> exampleAsyncTable)
            throws InterruptedException {
        LOG.info("Testing Async Dynamodb client with table: " + exampleAsyncTable.tableName());
        String partitionKeyAsString = UUID.randomUUID().toString();
        String rangeId = UUID.randomUUID().toString();

        DynamoDBExampleTableEntry exampleTableEntry = new DynamoDBExampleTableEntry();
        exampleTableEntry.setKeyId(partitionKeyAsString);
        exampleTableEntry.setRangeId(rangeId);
        exampleTableEntry.setPayload(PAYLOAD_VALUE);

        Key partitionKey = Key.builder().partitionValue(partitionKeyAsString).build();

        return exampleAsyncTable.createTable()
                .thenCompose(t -> exampleAsyncTable.putItem(exampleTableEntry))
                .thenCompose(t -> exampleAsyncTable.getItem(partitionKey))
                .thenApply(p -> p.getPayload() + "@" + p.getVersion())
                .exceptionally(th -> {
                    LOG.error("Error during async Dynamodb operations", th.getCause());
                    return "ERROR";
                });
    }

    protected String testBlockingDynamo(DynamoDbTable<DynamoDBExampleTableEntry> exampleBlockingTable) {
        LOG.info("Testing Blocking Dynamodb client with table: " + exampleBlockingTable.tableName());

        String partitionKeyAsString = UUID.randomUUID().toString();
        String rangeId = UUID.randomUUID().toString();

        exampleBlockingTable.createTable();

        DynamoDBExampleTableEntry exampleTableEntry = new DynamoDBExampleTableEntry();
        exampleTableEntry.setKeyId(partitionKeyAsString);
        exampleTableEntry.setRangeId(rangeId);
        exampleTableEntry.setPayload(PAYLOAD_VALUE);

        exampleBlockingTable.putItem(exampleTableEntry);

        Key partitionKey = Key.builder().partitionValue(partitionKeyAsString).build();

        DynamoDBExampleTableEntry existingTableEntry = exampleBlockingTable.getItem(partitionKey);

        if (existingTableEntry != null) {
            return existingTableEntry.getPayload() + "@" + existingTableEntry.getVersion();
        } else {
            return "ERROR";
        }
    }

    protected CompletionStage<String> testAsyncDynamoImmutable(
            DynamoDbAsyncTable<DynamoDBExampleTableEntryImmutable> exampleAsyncTable)
            throws InterruptedException {
        LOG.info("Testing Async Dynamodb client with table: " + exampleAsyncTable.tableName());
        String partitionKeyAsString = UUID.randomUUID().toString();
        String rangeId = UUID.randomUUID().toString();

        DynamoDBExampleTableEntryImmutable exampleTableEntry = DynamoDBExampleTableEntryImmutable.builder()
                .keyId(partitionKeyAsString).rangeId(rangeId).payload(PAYLOAD_VALUE).build();

        Key partitionKey = Key.builder().partitionValue(partitionKeyAsString).build();

        return exampleAsyncTable.createTable()
                .thenCompose(t -> exampleAsyncTable.putItem(exampleTableEntry))
                .thenCompose(t -> exampleAsyncTable.getItem(partitionKey))
                .thenApply(p -> p.getPayload() + "@" + p.getVersion())
                .exceptionally(th -> {
                    LOG.error("Error during async Dynamodb operations", th.getCause());
                    return "ERROR";
                });
    }

    protected String testBlockingDynamoImmuitable(DynamoDbTable<DynamoDBExampleTableEntryImmutable> exampleBlockingTable) {
        LOG.info("Testing Blocking Dynamodb client with table: " + exampleBlockingTable.tableName());

        String partitionKeyAsString = UUID.randomUUID().toString();
        String rangeId = UUID.randomUUID().toString();

        exampleBlockingTable.createTable();

        DynamoDBExampleTableEntryImmutable exampleTableEntry = DynamoDBExampleTableEntryImmutable.builder()
                .keyId(partitionKeyAsString).rangeId(rangeId).payload(PAYLOAD_VALUE).build();

        exampleBlockingTable.putItem(exampleTableEntry);

        Key partitionKey = Key.builder().partitionValue(partitionKeyAsString).build();

        DynamoDBExampleTableEntryImmutable existingTableEntry = exampleBlockingTable.getItem(partitionKey);

        if (existingTableEntry != null) {
            return existingTableEntry.getPayload() + "@" + existingTableEntry.getVersion();
        } else {
            return "ERROR";
        }
    }
}
