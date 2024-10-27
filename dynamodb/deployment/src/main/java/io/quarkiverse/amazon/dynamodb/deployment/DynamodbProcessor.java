package io.quarkiverse.amazon.dynamodb.deployment;

import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkiverse.amazon.common.deployment.AbstractAmazonServiceProcessor;
import io.quarkiverse.amazon.common.deployment.AmazonClientAsyncResultBuildItem;
import io.quarkiverse.amazon.common.deployment.AmazonClientAsyncTransportBuildItem;
import io.quarkiverse.amazon.common.deployment.AmazonClientInterceptorsPathBuildItem;
import io.quarkiverse.amazon.common.deployment.AmazonClientSyncResultBuildItem;
import io.quarkiverse.amazon.common.deployment.AmazonClientSyncTransportBuildItem;
import io.quarkiverse.amazon.common.deployment.AmazonHttpClients;
import io.quarkiverse.amazon.common.deployment.RequireAmazonClientBuildItem;
import io.quarkiverse.amazon.common.deployment.RequireAmazonClientInjectionBuildItem;
import io.quarkiverse.amazon.common.deployment.RequireAmazonClientTransportBuilderBuildItem;
import io.quarkiverse.amazon.common.deployment.RequireAmazonTelemetryBuildItem;
import io.quarkiverse.amazon.common.deployment.spi.EventLoopGroupBuildItem;
import io.quarkiverse.amazon.common.runtime.AmazonClientApacheTransportRecorder;
import io.quarkiverse.amazon.common.runtime.AmazonClientAwsCrtTransportRecorder;
import io.quarkiverse.amazon.common.runtime.AmazonClientCommonRecorder;
import io.quarkiverse.amazon.common.runtime.AmazonClientNettyTransportRecorder;
import io.quarkiverse.amazon.common.runtime.AmazonClientOpenTelemetryRecorder;
import io.quarkiverse.amazon.common.runtime.AmazonClientUrlConnectionTransportRecorder;
import io.quarkiverse.amazon.dynamodb.runtime.DynamodbBuildTimeConfig;
import io.quarkiverse.amazon.dynamodb.runtime.DynamodbRecorder;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

public class DynamodbProcessor extends AbstractAmazonServiceProcessor {

    private static final String AMAZON_CLIENT_NAME = "amazon-sdk-dynamodb";

    DynamodbBuildTimeConfig buildTimeConfig;

    @Override
    protected String amazonServiceClientName() {
        return AMAZON_CLIENT_NAME;
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
    void discoverClientInjectionPoints(BeanRegistrationPhaseBuildItem beanRegistrationPhase,
            BuildProducer<RequireAmazonClientInjectionBuildItem> requireClientInjectionProducer) {

        discoverClientInjectionPointsInternal(beanRegistrationPhase, requireClientInjectionProducer);
    }

    @BuildStep
    void discover(
            List<RequireAmazonClientInjectionBuildItem> amazonClientInjectionPoints,
            BuildProducer<RequireAmazonClientBuildItem> requireClientProducer) {

        discoverClient(amazonClientInjectionPoints, requireClientProducer);
    }

    @BuildStep
    void discoverTelemetry(BuildProducer<RequireAmazonTelemetryBuildItem> telemetryProducer) {

        discoverTelemetry(telemetryProducer, buildTimeConfig.sdk());
    }

    @BuildStep
    void setupClient(List<RequireAmazonClientBuildItem> clientRequirements,
            BuildProducer<RequireAmazonClientTransportBuilderBuildItem> clientProducer) {

        setupClient(clientRequirements, clientProducer, buildTimeConfig.sdk(), buildTimeConfig.syncClient(),
                buildTimeConfig.asyncClient());
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonApacheHttpServicePresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupApacheSyncTransport(List<RequireAmazonClientTransportBuilderBuildItem> amazonClients, DynamodbRecorder recorder,
            AmazonClientApacheTransportRecorder transportRecorder,
            BuildProducer<AmazonClientSyncTransportBuildItem> syncTransports) {

        createApacheSyncTransportBuilder(amazonClients,
                transportRecorder,
                buildTimeConfig.syncClient(),
                recorder.getSyncConfig(),
                syncTransports);
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonAwsCrtHttpServicePresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupAwsCrtSyncTransport(List<RequireAmazonClientTransportBuilderBuildItem> amazonClients, DynamodbRecorder recorder,
            AmazonClientAwsCrtTransportRecorder transportRecorder,
            BuildProducer<AmazonClientSyncTransportBuildItem> syncTransports) {

        createAwsCrtSyncTransportBuilder(amazonClients,
                transportRecorder,
                buildTimeConfig.syncClient(),
                recorder.getSyncConfig(),
                syncTransports);
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonUrlConnectionHttpServicePresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupUrlConnectionSyncTransport(List<RequireAmazonClientTransportBuilderBuildItem> amazonClients,
            DynamodbRecorder recorder,
            AmazonClientUrlConnectionTransportRecorder transportRecorder,
            BuildProducer<AmazonClientSyncTransportBuildItem> syncTransports) {

        createUrlConnectionSyncTransportBuilder(amazonClients,
                transportRecorder,
                buildTimeConfig.syncClient(),
                recorder.getSyncConfig(),
                syncTransports);
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonNettyHttpServicePresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupNettyAsyncTransport(List<RequireAmazonClientTransportBuilderBuildItem> amazonClients, DynamodbRecorder recorder,
            AmazonClientNettyTransportRecorder transportRecorder,
            BuildProducer<AmazonClientAsyncTransportBuildItem> asyncTransports,
            EventLoopGroupBuildItem eventLoopSupplier) {

        createNettyAsyncTransportBuilder(amazonClients,
                transportRecorder,
                buildTimeConfig.asyncClient(),
                recorder.getAsyncConfig(),
                asyncTransports, eventLoopSupplier.getMainEventLoopGroup());
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonAwsCrtHttpServicePresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupAwsCrtAsyncTransport(List<RequireAmazonClientTransportBuilderBuildItem> amazonClients, DynamodbRecorder recorder,
            AmazonClientAwsCrtTransportRecorder transportRecorder,
            BuildProducer<AmazonClientAsyncTransportBuildItem> asyncTransports) {

        createAwsCrtAsyncTransportBuilder(amazonClients,
                transportRecorder,
                buildTimeConfig.asyncClient(),
                recorder.getAsyncConfig(),
                asyncTransports);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void createClientBuilders(DynamodbRecorder recorder,
            AmazonClientCommonRecorder commonRecorder,
            AmazonClientOpenTelemetryRecorder otelRecorder,
            List<RequireAmazonClientInjectionBuildItem> amazonClientInjections,
            List<RequireAmazonTelemetryBuildItem> amazonRequireTelemtryClients,
            List<AmazonClientSyncTransportBuildItem> syncTransports,
            List<AmazonClientAsyncTransportBuildItem> asyncTransports,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<AmazonClientSyncResultBuildItem> clientSync,
            BuildProducer<AmazonClientAsyncResultBuildItem> clientAsync,
            LaunchModeBuildItem launchModeBuildItem,
            ExecutorBuildItem executorBuildItem) {

        createClientBuilders(
                recorder,
                commonRecorder,
                otelRecorder,
                buildTimeConfig,
                amazonClientInjections,
                amazonRequireTelemtryClients,
                syncTransports,
                asyncTransports,
                DynamoDbClientBuilder.class,
                DynamoDbAsyncClientBuilder.class,
                null,
                syntheticBeans,
                clientSync,
                clientAsync,
                launchModeBuildItem,
                executorBuildItem);
    }
}
