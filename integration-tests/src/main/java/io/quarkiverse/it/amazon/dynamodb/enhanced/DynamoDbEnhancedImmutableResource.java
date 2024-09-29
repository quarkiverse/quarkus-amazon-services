package io.quarkiverse.it.amazon.dynamodb.enhanced;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Path("/dynamodbenhancedimmutable")
public class DynamoDbEnhancedImmutableResource extends DynamoDbEnhancedAbstractResource {

    private final static String ASYNC_TABLE = "enhancedasyncimmutable";
    private final static String BLOCKING_TABLE = "enhancedblockingimmutable";

    @Inject
    DynamoDbEnhancedClient dynamoEnhancedClient;

    @Inject
    DynamoDbEnhancedAsyncClient dynamoEnhancedAsyncClient;

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsyncDynamo() throws InterruptedException {
        // when quarkus.dynamodbenhanced.create-table-schemas is true (default), TableSchema are cached at startup
        DynamoDbAsyncTable<DynamoDBExampleTableEntryImmutable> exampleAsyncTableFromClient = dynamoEnhancedAsyncClient.table(
                ASYNC_TABLE,
                TableSchema
                        .fromClass(DynamoDBExampleTableEntryImmutable.class));
        return testAsyncDynamoImmutable(exampleAsyncTableFromClient);
    }

    @GET
    @Path("blocking")
    @Produces(TEXT_PLAIN)
    public String testBlockingDynamo() {
        DynamoDbTable<DynamoDBExampleTableEntryImmutable> exampleBlockingTableFromClient = dynamoEnhancedClient.table(
                BLOCKING_TABLE,
                TableSchema
                        .fromClass(DynamoDBExampleTableEntryImmutable.class));
        return testBlockingDynamoImmuitable(exampleBlockingTableFromClient);
    }
}
