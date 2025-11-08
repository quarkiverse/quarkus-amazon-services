package io.quarkiverse.amazon.dynamodb.enhanced.deployment;

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
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem.ExtendedBeanConfigurator;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;

public class DynamoDbEnhancedDbTableProcessor {

    @BuildStep
    AdditionalBeanBuildItem additionalBeans() {
        return new AdditionalBeanBuildItem(NamedDynamoDbTable.class);
    }

    @BuildStep
    void discoverDynamoDbTable(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<DynamoDbEnhancedTableBuildItem> tables) {

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
                    tables.produce(new DynamoDbEnhancedTableBuildItem(tableName, beanClassName,
                            DotNames.DYNAMODB_ENHANCED_CLIENT, DotNames.DYNAMODB_TABLE));
                }
            }
            if (DotNames.DYNAMODB_ASYNC_TABLE.equals(dbTableClassName)) {
                if (asyncSeen.add(Map.entry(tableName, beanClassName))) {
                    tables.produce(new DynamoDbEnhancedTableBuildItem(tableName, beanClassName,
                            DotNames.DYNAMODB_ENHANCED_ASYNC_CLIENT, DotNames.DYNAMODB_ASYNC_TABLE));
                }
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void produceNamedDbTableBean(List<DynamoDbEnhancedTableBuildItem> tables,
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

    static private SyntheticBeanBuildItem generateDynamoDbTableSyntheticBean(DynamoDbEnhancedTableBuildItem table,
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
