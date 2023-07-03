package io.quarkus.amazon.dynamodb.enhanced.deployment;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.ParameterizedType;

import io.quarkus.amazon.common.deployment.RequireAmazonClientBuildItem;
import io.quarkus.amazon.dynamodb.enhanced.runtime.NamedDynamoDbTable;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

public class DynamodbEnhancedDbTableProcessor {

    //  <R> R getInjectedReference(Class<R> requiredType, Annotation... qualifiers);
    public static final MethodDescriptor CREATION_CONTEXT_GET_INJECTED_REFERENCE_METHOD = MethodDescriptor.ofMethod(
            SyntheticCreationalContext.class,
            "getInjectedReference", Object.class, Class.class, Annotation[].class);

    // static <T> TableSchema<T> fromClass(Class<T> annotatedClass)
    public static final MethodDescriptor TABLE_SCHEMA_FROM_CLASS_METHOD = MethodDescriptor.ofMethod(TableSchema.class,
            "fromClass", TableSchema.class,
            Class.class);

    // <T> DynamoDbTable<T> table(String tableName, TableSchema<T> tableSchema);
    public static final MethodDescriptor DYNAMODB_ENHANCED_CLIENT_TABLE_METHOD = MethodDescriptor.ofMethod(
            DynamoDbEnhancedClient.class, "table",
            DynamoDbTable.class, String.class, TableSchema.class);
    // <T> DynamoDbAsyncTable<T> table(String tableName, TableSchema<T> tableSchema);
    public static final MethodDescriptor DYNAMODB_ENHANCED_ASYNC_CLIENT_TABLE_METHOD = MethodDescriptor.ofMethod(
            DynamoDbEnhancedAsyncClient.class, "table",
            DynamoDbAsyncTable.class, String.class, TableSchema.class);

    @BuildStep
    AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem.unremovableOf(NamedDynamoDbTable.class);
    }

    @BuildStep
    void discoverDynamoDbTable(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<DynamodbEnhancedTableBuildItem> tables,
            BuildProducer<RequireAmazonClientBuildItem> requireClientProducer) {

        Optional<DotName> syncClassName = Optional.empty();
        Optional<DotName> asyncClassName = Optional.empty();

        Set<Map.Entry<String, DotName>> asyncSeen = new HashSet<>();
        Set<Map.Entry<String, DotName>> syncSeen = new HashSet<>();

        IndexView index = combinedIndexBuildItem.getIndex();
        Collection<AnnotationInstance> ais = index.getAnnotations(DotNames.DYNAMODB_NAMED_TABLE);
        for (AnnotationInstance ano : ais) {
            if (ano.target().kind().equals(Kind.FIELD)) {
                FieldInfo field = ano.target().asField();
                String tableName = ano.value().asString();
                DotName beanClassName = field.type().asParameterizedType().arguments().get(0).name();

                ClassInfo beanClass = index.getClassByName(beanClassName);
                if (beanClass.annotation(DotNames.DYNAMODB_ENHANCED_BEAN) == null
                        && beanClass.annotation(DotNames.DYNAMODB_ENHANCED_IMMUTABLE) == null) {
                    throw new DeploymentException(String
                            .format("'%s' must be bean annotated with @DynamoDbBean or @DynamoDbImmutable", beanClassName));
                }

                if (DotNames.DYNAMODB_TABLE.equals(field.type().name())) {
                    if (syncSeen.add(Map.entry(tableName, beanClassName))) {
                        tables.produce(new DynamodbEnhancedTableBuildItem(tableName, beanClassName,
                                DotNames.DYNAMODB_ENHANCED_CLIENT, DYNAMODB_ENHANCED_CLIENT_TABLE_METHOD,
                                DotNames.DYNAMODB_TABLE));
                        syncClassName = Optional.of(DotNames.DYNAMODB_CLIENT);
                    }
                }
                if (DotNames.DYNAMODB_ASYNC_TABLE.equals(field.type().name())) {
                    if (asyncSeen.add(Map.entry(tableName, beanClassName))) {
                        tables.produce(new DynamodbEnhancedTableBuildItem(tableName, beanClassName,
                                DotNames.DYNAMODB_ENHANCED_ASYNC_CLIENT, DYNAMODB_ENHANCED_ASYNC_CLIENT_TABLE_METHOD,
                                DotNames.DYNAMODB_ASYNC_TABLE));
                        asyncClassName = Optional.of(DotNames.DYNAMODB_ASYNC_CLIENT);
                    }
                }
            }
        }

        if (syncClassName.isPresent() || asyncClassName.isPresent()) {
            requireClientProducer.produce(new RequireAmazonClientBuildItem(syncClassName, asyncClassName));
        }
    }

    @BuildStep
    public void produceNamedDbTableBean(List<DynamodbEnhancedTableBuildItem> tables,
            BuildProducer<SyntheticBeanBuildItem> syntheticBean) {

        tables.stream().map(DynamodbEnhancedDbTableProcessor::generateDynamoDbTableSyntheticBean)
                .forEach(syntheticBean::produce);
    }

    static private SyntheticBeanBuildItem generateDynamoDbTableSyntheticBean(DynamodbEnhancedTableBuildItem table) {
        return SyntheticBeanBuildItem
                .configure(table.getTableClassName())
                .addType(ParameterizedType.builder(table.getTableClassName())
                        .addArgument(ClassType.create(table.getBeanClassName())).build())
                .scope(Singleton.class)
                .qualifiers(AnnotationInstance.builder(NamedDynamoDbTable.class).value(table.getTableName()).build())
                .unremovable()
                .creator(methodCreator -> {
                    generateDynamoDbTableSyncTableProducerMethod(methodCreator, table);
                })
                .addInjectionPoint(ClassType.create(table.getClientClassName()))
                .done();
    }

    static private void generateDynamoDbTableSyncTableProducerMethod(MethodCreator methodCreator,
            DynamodbEnhancedTableBuildItem table) {
        // DynamoDbEnhancedClient dynamoEnhancedClient = arg0.getInjectedReferenceMethod(DynamoDbEnhancedClient.class, {})
        // String tableName = "...";
        // Class beanClass = Class.forName("...", TCCL)
        // TableSchema tableSchema = TableSchema.fromClass(beanClass)
        // dynamoEnhancedClient.table(tableName, tableSchema)
        var dynamoEnhancedClientHandle = methodCreator.invokeInterfaceMethod(CREATION_CONTEXT_GET_INJECTED_REFERENCE_METHOD,
                methodCreator.getMethodParam(0), methodCreator.loadClass(table.getClientClassName().toString()),
                methodCreator.newArray(Annotation.class, 0));
        var tableNameHandler = methodCreator.load(table.getTableName());
        var beanClassHandler = methodCreator.loadClassFromTCCL(table.getBeanClassName().toString());
        var tableSchemaHandle = methodCreator.invokeStaticInterfaceMethod(TABLE_SCHEMA_FROM_CLASS_METHOD,
                beanClassHandler);
        var mappedTableHandle = methodCreator.invokeInterfaceMethod(table.getTableMethodDescriptor(),
                dynamoEnhancedClientHandle, tableNameHandler,
                tableSchemaHandle);

        methodCreator.returnValue(mappedTableHandle);
    }
}
