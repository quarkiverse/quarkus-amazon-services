package io.quarkus.it.amazon.dynamodb.enhanced;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.logging.Logger;

import software.amazon.awssdk.enhanced.dynamodb.*;

@Path("/dynamodbenhanced")
public class DynamoDbEnhancedResource {
    private final static String ASYNC_TABLE = "enhancedasync";
    private final static String BLOCKING_TABLE = "enhancedblocking";

    private static final Logger LOG = Logger.getLogger(DynamoDbEnhancedResource.class);

    private final static String PAYLOAD_VALUE = "OK";

    // when quarkus.dynamodbenhanced.create-table-schemas is true (default), this is not necessary
    private static final TableSchema<DynamoDBExampleTableEntry> TABLE_SCHEMA = TableSchema
            .fromClass(DynamoDBExampleTableEntry.class);

    @Inject
    DynamoDbEnhancedClient dynamoEnhancedClient;

    @Inject
    DynamoDbEnhancedAsyncClient dynamoEnhancedAsyncClient;

    private DynamoDbTable<DynamoDBExampleTableEntry> exampleBlockingTable;

    @PostConstruct
    void init() {
        this.exampleBlockingTable = dynamoEnhancedClient.table(BLOCKING_TABLE, TABLE_SCHEMA);
    }

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsyncDynamo() throws InterruptedException {
        LOG.info("Testing Async Dynamodb client with table: " + ASYNC_TABLE);
        String partitionKeyAsString = UUID.randomUUID().toString();
        String rangeId = UUID.randomUUID().toString();

        DynamoDbAsyncTable<DynamoDBExampleTableEntry> exampleAsyncTable = dynamoEnhancedAsyncClient.table(ASYNC_TABLE,
                TABLE_SCHEMA);

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

    @GET
    @Path("blocking")
    @Produces(TEXT_PLAIN)
    public String testBlockingDynamo() {
        LOG.info("Testing Blocking Dynamodb client with table: " + BLOCKING_TABLE);

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
}
