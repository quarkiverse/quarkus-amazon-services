package io.quarkus.amazon.dynamodb.enhanced.runtime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.client.ExtensionResolver;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Recorder
public class DynamodbEnhancedClientRecorder {
    private static final Log LOG = LogFactory.getLog(DynamodbEnhancedClientRecorder.class);

    DynamoDbEnhancedBuildTimeConfig buildTimeConfig;

    public DynamodbEnhancedClientRecorder(DynamoDbEnhancedBuildTimeConfig buildTimeConfig) {
        this.buildTimeConfig = buildTimeConfig;
    }

    public void createTableSchema(List<Class<?>> tableSchemClasses) {
        for (Class<?> tableSchemaClass : tableSchemClasses) {
            TableSchema.fromClass(tableSchemaClass);
        }
    }

    public RuntimeValue<DynamoDbEnhancedClientExtension> createExtensionList() {

        List<DynamoDbEnhancedClientExtension> extensions = new ArrayList<>();
        for (String item : buildTimeConfig.clientExtensions().orElse(Collections.emptyList())) {
            DynamoDbEnhancedClientExtension extension = createExtension(item.trim());
            if (Objects.nonNull(extension)) {
                extensions.add(extension);
            }
        }
        if (extensions.isEmpty()) {
            extensions.addAll(ExtensionResolver.defaultExtensions());
        }
        return new RuntimeValue<>(ExtensionResolver.resolveExtensions(extensions));
    }

    private DynamoDbEnhancedClientExtension createExtension(String extensionClassName) {

        Class<?> clazz = null;
        try {
            clazz = Class.forName(extensionClassName, false, Thread.currentThread().getContextClassLoader());
            // try builder pattern in aws sdk
            Method builderMethod = clazz.getMethod("builder");
            Object builder = builderMethod.invoke(null);
            DynamoDbEnhancedClientExtension extension = (DynamoDbEnhancedClientExtension) builder.getClass().getMethod("build")
                    .invoke(builder);
            return extension;
        } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException e) {
            LOG.error("Unable to create extension " + extensionClassName, e);
            return null;
        } catch (NoSuchMethodException e) {
            try {
                return (DynamoDbEnhancedClientExtension) clazz.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                LOG.error("Unable to create extension " + extensionClassName, e);
                return null;
            }
        }
    }

    public Function<SyntheticCreationalContext<DynamoDbEnhancedClient>, DynamoDbEnhancedClient> createDynamoDbEnhancedClient(
            RuntimeValue<DynamoDbEnhancedClientExtension> extensions) {
        return new Function<SyntheticCreationalContext<DynamoDbEnhancedClient>, DynamoDbEnhancedClient>() {
            @Override
            public DynamoDbEnhancedClient apply(SyntheticCreationalContext<DynamoDbEnhancedClient> context) {
                return DynamoDbEnhancedClient.builder().dynamoDbClient(context.getInjectedReference(DynamoDbClient.class))
                        .extensions(extensions.getValue()).build();
            }
        };
    }

    public Function<SyntheticCreationalContext<DynamoDbEnhancedAsyncClient>, DynamoDbEnhancedAsyncClient> createDynamoDbEnhancedAsyncClient(
            RuntimeValue<DynamoDbEnhancedClientExtension> extensions) {
        return new Function<SyntheticCreationalContext<DynamoDbEnhancedAsyncClient>, DynamoDbEnhancedAsyncClient>() {
            @Override
            public DynamoDbEnhancedAsyncClient apply(SyntheticCreationalContext<DynamoDbEnhancedAsyncClient> context) {
                return DynamoDbEnhancedAsyncClient.builder()
                        .dynamoDbClient(context.getInjectedReference(DynamoDbAsyncClient.class))
                        .extensions(extensions.getValue()).build();
            }
        };
    }
}
