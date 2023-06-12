package io.quarkus.amazon.dynamodb.deployment;

import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkus.amazon.common.deployment.AbstractAmazonServiceProcessor;
import io.quarkus.amazon.common.deployment.AmazonClientAsyncResultBuildItem;
import io.quarkus.amazon.common.deployment.AmazonClientAsyncTransportBuildItem;
import io.quarkus.amazon.common.deployment.AmazonClientBuildItem;
import io.quarkus.amazon.common.deployment.AmazonClientInterceptorsPathBuildItem;
import io.quarkus.amazon.common.deployment.AmazonClientSyncResultBuildItem;
import io.quarkus.amazon.common.deployment.AmazonClientSyncTransportBuildItem;
import io.quarkus.amazon.common.deployment.AmazonHttpClients;
import io.quarkus.amazon.common.deployment.RequireAmazonClientBuildItem;
import io.quarkus.amazon.common.runtime.AmazonClientApacheTransportRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientNettyTransportRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientUrlConnectionTransportRecorder;
import io.quarkus.amazon.dynamodb.runtime.DynamodbBuildTimeConfig;
import io.quarkus.amazon.dynamodb.runtime.DynamodbClientProducer;
import io.quarkus.amazon.dynamodb.runtime.DynamodbRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

public class DynamodbProcessor extends AbstractAmazonServiceProcessor {

    private static final String AMAZON_DYNAMODB = "amazon-dynamodb";

    DynamodbBuildTimeConfig buildTimeConfig;

    @Override
    protected String amazonServiceClientName() {
        return AMAZON_DYNAMODB;
    }

    @Override
    protected String configName() {
        return "dynamodb";
    }

    @Override
    protected DotName syncClientName() {
        return DotName.createSimple(DynamoDbClient.class.getName());
    }

    @Override
    protected DotName asyncClientName() {
        return DotName.createSimple(DynamoDbAsyncClient.class.getName());
    }

    @Override
    protected String builtinInterceptorsPath() {
        return "software/amazon/awssdk/services/dynamodb/execution.interceptors";
    }

    @BuildStep
    AdditionalBeanBuildItem producer() {
        return AdditionalBeanBuildItem.unremovableOf(DynamodbClientProducer.class);
    }

    @BuildStep
    void runtimeInitialize(BuildProducer<RuntimeInitializedClassBuildItem> producer) {
        // This class triggers initialization of FullJitterBackoffStragegy so needs to get runtime-initialized
        // as well
        producer.produce(
                new RuntimeInitializedClassBuildItem("software.amazon.awssdk.services.dynamodb.DynamoDbRetryPolicy"));
    }

    @BuildStep
    void setup(
            BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<AmazonClientInterceptorsPathBuildItem> interceptors) {

        setupExtension(extensionSslNativeSupport, feature, interceptors);
    }

    @BuildStep
    void discover(BeanRegistrationPhaseBuildItem beanRegistrationPhase,
            BuildProducer<RequireAmazonClientBuildItem> requireClientProducer) {

        discoverClient(beanRegistrationPhase, requireClientProducer);
    }

    @BuildStep
    void setupClient(List<RequireAmazonClientBuildItem> clientRequirements,
            BuildProducer<AmazonClientBuildItem> clientProducer) {

        setupClient(clientRequirements, clientProducer, buildTimeConfig.sdk, buildTimeConfig.syncClient);
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonApacheHttpServicePresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupApacheSyncTransport(List<AmazonClientBuildItem> amazonClients, DynamodbRecorder recorder,
            AmazonClientApacheTransportRecorder transportRecorder,
            BuildProducer<AmazonClientSyncTransportBuildItem> syncTransports) {

        createApacheSyncTransportBuilder(amazonClients,
                transportRecorder,
                buildTimeConfig.syncClient,
                recorder.getSyncConfig(),
                syncTransports);
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonUrlConnectionHttpServicePresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupUrlConnectionSyncTransport(List<AmazonClientBuildItem> amazonClients, DynamodbRecorder recorder,
            AmazonClientUrlConnectionTransportRecorder transportRecorder,
            BuildProducer<AmazonClientSyncTransportBuildItem> syncTransports) {

        createUrlConnectionSyncTransportBuilder(amazonClients,
                transportRecorder,
                buildTimeConfig.syncClient,
                recorder.getSyncConfig(),
                syncTransports);
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonNettyHttpServicePresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupNettyAsyncTransport(List<AmazonClientBuildItem> amazonClients, DynamodbRecorder recorder,
            AmazonClientNettyTransportRecorder transportRecorder,
            BuildProducer<AmazonClientAsyncTransportBuildItem> asyncTransports) {

        createNettyAsyncTransportBuilder(amazonClients,
                transportRecorder,
                recorder.getAsyncConfig(),
                asyncTransports);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void createClientBuilders(DynamodbRecorder recorder,
            AmazonClientRecorder commonRecorder,
            List<AmazonClientSyncTransportBuildItem> syncTransports,
            List<AmazonClientAsyncTransportBuildItem> asyncTransports,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<AmazonClientSyncResultBuildItem> clientSync,
            BuildProducer<AmazonClientAsyncResultBuildItem> clientAsync) {

        createClientBuilders(commonRecorder,
                recorder.getAwsConfig(),
                recorder.getSdkConfig(),
                buildTimeConfig.sdk,
                syncTransports,
                asyncTransports,
                DynamoDbClientBuilder.class,
                (syncTransport) -> recorder.createSyncBuilder(syncTransport),
                DynamoDbAsyncClientBuilder.class,
                (asyncTransport) -> recorder.createAsyncBuilder(asyncTransport),
                null,
                null,
                syntheticBeans,
                clientSync,
                clientAsync);
    }
}
