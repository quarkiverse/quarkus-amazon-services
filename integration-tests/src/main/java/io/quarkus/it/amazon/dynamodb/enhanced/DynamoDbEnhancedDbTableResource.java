package io.quarkus.it.amazon.dynamodb.enhanced;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import io.quarkus.amazon.dynamodb.enhanced.runtime.NamedDynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.*;

@Path("/dynamodbenhanceddbtable")
public class DynamoDbEnhancedDbTableResource extends DynamoDbEnhancedAbstractResource {

    private final static String ASYNC_TABLE = "enhancedasyncdbtable";
    private final static String BLOCKING_TABLE = "enhancedblockingdbtable";

    @NamedDynamoDbTable(BLOCKING_TABLE)
    @Inject
    DynamoDbTable<DynamoDBExampleTableEntry> exampleBlockingTable;

    @NamedDynamoDbTable(ASYNC_TABLE)
    @Inject
    DynamoDbAsyncTable<DynamoDBExampleTableEntry> exampleAsyncTable;

    @GET
    @Path("async")
    @Produces(TEXT_PLAIN)
    public CompletionStage<String> testAsyncDynamo() throws InterruptedException {
        return testAsyncDynamo(exampleAsyncTable);
    }

    @GET
    @Path("blocking")
    @Produces(TEXT_PLAIN)
    public String testBlockingDynamo() {
        return testBlockingDynamo(exampleBlockingTable);
    }
}
