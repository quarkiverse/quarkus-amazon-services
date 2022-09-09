package io.quarkus.it.amazon.dynamodb.enhanced;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.jboss.logging.Logger;

import software.amazon.awssdk.enhanced.dynamodb.*;

@Path("/dynamodbenhanced")

public class DynamoDbEnhancedResource {
    private final static String ASYNC_TABLE = "enhancedasync";
    private final static String BLOCKING_TABLE = "enhancedblocking";

    private static final Logger LOG = Logger.getLogger(DynamoDbEnhancedResource.class);

    private final static String PAYLOAD_VALUE = "OK";

    @Inject
    DynamoDbEnhancedClient dynamoEnhancedClient;

    @Inject
    DynamoDbEnhancedAsyncClient dynamoEnhancedAsyncClient;

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsyncDynamo() throws InterruptedException {
        LOG.info("Testing Async Dynamodb client with table: " + ASYNC_TABLE);
        String partitionKeyAsString = UUID.randomUUID().toString();
        String rangeId = UUID.randomUUID().toString();

        DynamoDbAsyncTable<DynamoDBExampleTableEntry> exampleAsyncTable = dynamoEnhancedAsyncClient.table(ASYNC_TABLE,
                TableSchema.fromClass(DynamoDBExampleTableEntry.class));

        DynamoDBExampleTableEntry exampleTableEntry = new DynamoDBExampleTableEntry();
        exampleTableEntry.setKeyId(partitionKeyAsString);
        exampleTableEntry.setRangeId(rangeId);
        exampleTableEntry.setPayload(PAYLOAD_VALUE);

        Key partitionKey = Key.builder().partitionValue(partitionKeyAsString).build();

        return exampleAsyncTable.createTable()
                .thenCompose(t -> exampleAsyncTable.putItem(exampleTableEntry))
                .thenCompose(t -> exampleAsyncTable.getItem(partitionKey))
                .thenApply(p -> p.getPayload())
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

        DynamoDbTable<DynamoDBExampleTableEntry> exampleBlockingTable = dynamoEnhancedClient.table(BLOCKING_TABLE,
                TableSchema.fromClass(DynamoDBExampleTableEntry.class));

        exampleBlockingTable.createTable();

        DynamoDBExampleTableEntry exampleTableEntry = new DynamoDBExampleTableEntry();
        exampleTableEntry.setKeyId(partitionKeyAsString);
        exampleTableEntry.setRangeId(rangeId);
        exampleTableEntry.setPayload(PAYLOAD_VALUE);

        exampleBlockingTable.putItem(exampleTableEntry);

        Key partitionKey = Key.builder().partitionValue(partitionKeyAsString).build();

        DynamoDBExampleTableEntry existingTableEntry = exampleBlockingTable.getItem(partitionKey);

        if (existingTableEntry != null) {
            return existingTableEntry.getPayload();
        } else {
            return "ERROR";
        }
    }
}
