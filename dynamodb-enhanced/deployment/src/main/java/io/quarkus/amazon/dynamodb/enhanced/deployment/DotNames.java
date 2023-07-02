package io.quarkus.amazon.dynamodb.enhanced.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.amazon.dynamodb.enhanced.runtime.NamedDynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class DotNames {
    public static final DotName DYNAMODB_CLIENT = DotName.createSimple(DynamoDbClient.class);
    public static final DotName DYNAMODB_ASYNC_CLIENT = DotName
            .createSimple(DynamoDbAsyncClient.class);
    public static final DotName DYNAMODB_ENHANCED_CLIENT = DotName.createSimple(DynamoDbEnhancedClient.class);
    public static final DotName DYNAMODB_ENHANCED_ASYNC_CLIENT = DotName
            .createSimple(DynamoDbEnhancedAsyncClient.class);
    public static final DotName DYNAMODB_ENHANCED_CLIENT_EXTENSION_NAME = DotName
            .createSimple(DynamoDbEnhancedClientExtension.class);
    public static final DotName DYNAMODB_ENHANCED_BEAN = DotName.createSimple(DynamoDbBean.class);
    public static final DotName DYNAMODB_ENHANCED_IMMUTABLE = DotName.createSimple(DynamoDbImmutable.class);
    public static final DotName DYNAMODB_NAMED_TABLE = DotName.createSimple(NamedDynamoDbTable.class);
    public static final DotName DYNAMODB_TABLE = DotName.createSimple(DynamoDbTable.class);
    public static final DotName DYNAMODB_ASYNC_TABLE = DotName.createSimple(DynamoDbAsyncTable.class);

    private DotNames() {
    }
}
