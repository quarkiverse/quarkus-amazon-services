package io.quarkus.amazon.eventbridge.deployment;

import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkus.amazon.common.deployment.AbstractAmazonServiceProcessor;
import io.quarkus.amazon.common.deployment.AmazonClientAsyncResultBuildItem;
import io.quarkus.amazon.common.deployment.AmazonClientAsyncTransportBuildItem;
import io.quarkus.amazon.common.deployment.AmazonClientInterceptorsPathBuildItem;
import io.quarkus.amazon.common.deployment.AmazonClientSyncResultBuildItem;
import io.quarkus.amazon.common.deployment.AmazonClientSyncTransportBuildItem;
import io.quarkus.amazon.common.deployment.AmazonHttpClients;
import io.quarkus.amazon.common.deployment.RequireAmazonClientBuildItem;
import io.quarkus.amazon.common.deployment.RequireAmazonClientInjectionBuildItem;
import io.quarkus.amazon.common.deployment.RequireAmazonClientTransportBuilderBuildItem;
import io.quarkus.amazon.common.deployment.RequireAmazonTelemetryBuildItem;
import io.quarkus.amazon.common.deployment.spi.EventLoopGroupBuildItem;
import io.quarkus.amazon.common.runtime.AmazonClientApacheTransportRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientAwsCrtTransportRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientCommonRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientNettyTransportRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientOpenTelemetryRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientUrlConnectionTransportRecorder;
import io.quarkus.amazon.eventbridge.runtime.EventBridgeBuildTimeConfig;
import io.quarkus.amazon.eventbridge.runtime.EventBridgeRecorder;
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
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClientBuilder;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.EventBridgeClientBuilder;

public class EventBridgeProcessor extends AbstractAmazonServiceProcessor {

    private static final String AMAZON_KINESIS = "amazon-eventbridge";

    EventBridgeBuildTimeConfig buildTimeConfig;

    @Override
    protected String amazonServiceClientName() {
        return AMAZON_KINESIS;
    }

    @Override
    protected String configName() {
        return "eventbridge";
    }

    @Override
    protected DotName syncClientName() {
        return DotName.createSimple(EventBridgeClient.class.getName());
    }

    @Override
    protected DotName asyncClientName() {
        return DotName.createSimple(EventBridgeAsyncClient.class.getName());
    }

    @Override
    protected String builtinInterceptorsPath() {
        return "software/amazon/awssdk/services/eventbridge/execution.interceptors";
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
    void setupApacheSyncTransport(List<RequireAmazonClientTransportBuilderBuildItem> amazonClients,
            EventBridgeRecorder recorder,
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
    void setupAwsCrtSyncTransport(List<RequireAmazonClientTransportBuilderBuildItem> amazonClients,
            EventBridgeRecorder recorder,
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
            EventBridgeRecorder recorder,
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
    void setupNettyAsyncTransport(List<RequireAmazonClientTransportBuilderBuildItem> amazonClients,
            EventBridgeRecorder recorder,
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
    void setupAwsCrtAsyncTransport(List<RequireAmazonClientTransportBuilderBuildItem> amazonClients,
            EventBridgeRecorder recorder,
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
    void createClientBuilders(EventBridgeRecorder recorder,
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
                EventBridgeClientBuilder.class,
                EventBridgeAsyncClientBuilder.class,
                null,
                syntheticBeans,
                clientSync,
                clientAsync,
                launchModeBuildItem,
                executorBuildItem);
    }
}
