package io.quarkiverse.amazon.dynamodb.enhanced.runtime;

import java.util.function.Function;

import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Recorder
public class DynamoDbEnhancedTableRecorder {

    public Function<SyntheticCreationalContext<DynamoDbTable<?>>, DynamoDbTable<?>> createDynamoDbTable() {
        return new Function<SyntheticCreationalContext<DynamoDbTable<?>>, DynamoDbTable<?>>() {
            @Override
            public DynamoDbTable<?> apply(SyntheticCreationalContext<DynamoDbTable<?>> creationalContext) {

                DynamoDbEnhancedClient dynamoEnhancedClient = creationalContext
                        .getInjectedReference(DynamoDbEnhancedClient.class);
                String beanClassName = creationalContext.getParams().get("beanClassName").toString();
                String tableName = creationalContext.getParams().get("tableName").toString();
                Class<?> beanClass;
                try {
                    beanClass = Thread.currentThread().getContextClassLoader().loadClass(beanClassName);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Failed to load class for DynamoDbTable: " + beanClassName, e);
                }
                TableSchema<?> tableSchema = TableSchema.fromClass(beanClass);
                return dynamoEnhancedClient.table(tableName, tableSchema);
            }
        };
    }

    public Function<SyntheticCreationalContext<DynamoDbAsyncTable<?>>, DynamoDbAsyncTable<?>> createDynamoDbAsyncTable() {
        return new Function<SyntheticCreationalContext<DynamoDbAsyncTable<?>>, DynamoDbAsyncTable<?>>() {
            @Override
            public DynamoDbAsyncTable<?> apply(SyntheticCreationalContext<DynamoDbAsyncTable<?>> creationalContext) {

                DynamoDbEnhancedAsyncClient dynamoEnhancedClient = creationalContext
                        .getInjectedReference(DynamoDbEnhancedAsyncClient.class);
                String beanClassName = creationalContext.getParams().get("beanClassName").toString();
                String tableName = creationalContext.getParams().get("tableName").toString();
                Class<?> beanClass;
                try {
                    beanClass = Thread.currentThread().getContextClassLoader().loadClass(beanClassName);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Failed to load class for DynamoDbTable: " + beanClassName, e);
                }
                TableSchema<?> tableSchema = TableSchema.fromClass(beanClass);
                return dynamoEnhancedClient.table(tableName, tableSchema);
            }
        };
    }
}
