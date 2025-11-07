package io.quarkiverse.amazon.dynamodb.enhanced.deployment;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import io.quarkiverse.amazon.common.deployment.RequireAmazonClientInjectionBuildItem;
import io.quarkiverse.amazon.common.runtime.ClientUtil;
import io.quarkiverse.amazon.dynamodb.enhanced.runtime.DynamoDbEnhancedTableRecorder;
import io.quarkiverse.amazon.dynamodb.enhanced.runtime.NamedDynamoDbTable;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem.ExtendedBeanConfigurator;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
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
        return new AdditionalBeanBuildItem(NamedDynamoDbTable.class);
    }

    @BuildStep
    void discoverDynamoDbTable(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<DynamodbEnhancedTableBuildItem> tables) {

        Set<Map.Entry<String, DotName>> asyncSeen = new HashSet<>();
        Set<Map.Entry<String, DotName>> syncSeen = new HashSet<>();

        IndexView index = combinedIndexBuildItem.getIndex();
        Collection<AnnotationInstance> ais = index.getAnnotations(DotNames.DYNAMODB_NAMED_TABLE);
        for (AnnotationInstance ano : ais) {
            String tableName = ano.value().asString();
            Type targetType = null;

            if (ano.target().kind().equals(Kind.FIELD)) {
                targetType = ano.target().asField().type();
            } else if (ano.target().kind().equals(Kind.METHOD_PARAMETER)) {
                MethodParameterInfo mpi = ano.target().asMethodParameter();
                if (mpi.method().isConstructor()) {
                    targetType = mpi.type();
                }
            }

            if (targetType == null) {
                continue;
            }

            DotName beanClassName = targetType.asParameterizedType().arguments().get(0).name();
            DotName dbTableClassName = targetType.name();

            ClassInfo beanClass = index.getClassByName(beanClassName);

            if (beanClass == null) {
                throw new DeploymentException(String
                        .format("'%s' is not in the Jandex index", beanClassName));
            }

            if (beanClass.annotation(DotNames.DYNAMODB_ENHANCED_BEAN) == null
                    && beanClass.annotation(DotNames.DYNAMODB_ENHANCED_IMMUTABLE) == null) {
                throw new DeploymentException(String
                        .format("'%s' must be bean annotated with @DynamoDbBean or @DynamoDbImmutable", beanClassName));
            }

            if (DotNames.DYNAMODB_TABLE.equals(dbTableClassName)) {
                if (syncSeen.add(Map.entry(tableName, beanClassName))) {
                    tables.produce(new DynamodbEnhancedTableBuildItem(tableName, beanClassName,
                            DotNames.DYNAMODB_ENHANCED_CLIENT, DYNAMODB_ENHANCED_CLIENT_TABLE_METHOD,
                            DotNames.DYNAMODB_TABLE));
                }
            }
            if (DotNames.DYNAMODB_ASYNC_TABLE.equals(dbTableClassName)) {
                if (asyncSeen.add(Map.entry(tableName, beanClassName))) {
                    tables.produce(new DynamodbEnhancedTableBuildItem(tableName, beanClassName,
                            DotNames.DYNAMODB_ENHANCED_ASYNC_CLIENT, DYNAMODB_ENHANCED_ASYNC_CLIENT_TABLE_METHOD,
                            DotNames.DYNAMODB_ASYNC_TABLE));
                }
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void produceNamedDbTableBean(List<DynamodbEnhancedTableBuildItem> tables,
            DynamoDbEnhancedTableRecorder recorder,
            BuildProducer<RequireAmazonClientInjectionBuildItem> requireClientInjectionProducer,
            BuildProducer<SyntheticBeanBuildItem> syntheticBean) {

        // produce a synthetic bean for each DynamoDb table bean
        tables.stream().map(table -> generateDynamoDbTableSyntheticBean(table, recorder))
                .forEach(syntheticBean::produce);
        // each table bean requires a DynamoDb Enhanced client
        tables.stream()
                .map(table -> new RequireAmazonClientInjectionBuildItem(table.getClientClassName(),
                        ClientUtil.DEFAULT_CLIENT_NAME))
                .forEach(requireClientInjectionProducer::produce);
        // which in turn require a regular low-level DynamoDb client
        tables.stream()
                .map(table -> new RequireAmazonClientInjectionBuildItem(getLowLevelClientClassName(table.getClientClassName()),
                        ClientUtil.DEFAULT_CLIENT_NAME))
                .forEach(requireClientInjectionProducer::produce);
    }

    public DotName getLowLevelClientClassName(DotName enhancedClientClassName) {
        if (DotNames.DYNAMODB_ENHANCED_CLIENT.equals(enhancedClientClassName)) {
            return DotNames.DYNAMODB_CLIENT;
        } else {
            return DotNames.DYNAMODB_ASYNC_CLIENT;
        }
    }

    static private SyntheticBeanBuildItem generateDynamoDbTableSyntheticBean(DynamodbEnhancedTableBuildItem table,
            DynamoDbEnhancedTableRecorder recorder) {
        // allows to @inject DynamoDbTable<DynamoDBExampleTableEntry> or  DynamoDbAsyncTable<DynamoDBExampleTableEntry>
        ExtendedBeanConfigurator beanConfigurator = SyntheticBeanBuildItem
                .configure(table.getTableClassName())
                .addType(ParameterizedType.builder(table.getTableClassName())
                        .addArgument(ClassType.create(table.getBeanClassName())).build())
                .scope(Singleton.class)
                .qualifiers(AnnotationInstance.builder(NamedDynamoDbTable.class).value(table.getTableName()).build())
                .unremovable()
                .param("tableName", table.getTableName())
                .param("beanClassName", table.getBeanClassName().toString())
                .addInjectionPoint(ClassType.create(table.getClientClassName()));

        if (DotNames.DYNAMODB_TABLE.equals(table.getTableClassName())) {
            beanConfigurator.createWith(recorder.createDynamoDbTable());
        } else {
            beanConfigurator.createWith(recorder.createDynamoDbAsyncTable());
        }

        return beanConfigurator.done();
    }
}
