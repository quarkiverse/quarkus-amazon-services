package io.quarkus.amazon.lambda.deployment;

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
import io.quarkus.amazon.common.deployment.spi.EventLoopGroupBuildItem;
import io.quarkus.amazon.common.runtime.AmazonClientApacheTransportRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientAwsCrtTransportRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientCommonRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientNettyTransportRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientUrlConnectionTransportRecorder;
import io.quarkus.amazon.lambda.runtime.LambdaBuildTimeConfig;
import io.quarkus.amazon.lambda.runtime.LambdaConfig;
import io.quarkus.amazon.lambda.runtime.LambdaProducer;
import io.quarkus.amazon.lambda.runtime.LambdaRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.LambdaAsyncClientBuilder;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.LambdaClientBuilder;

public class LambdaProcessor extends AbstractAmazonServiceProcessor {
    private static final String AMAZON_Lambda = "amazon-lambda";

    LambdaBuildTimeConfig buildTimeConfig;

    @Override
    protected String amazonServiceClientName() {
        return AMAZON_Lambda;
    }

    @Override
    protected String configName() {
        return "lambda";
    }

    @Override
    protected DotName syncClientName() {
        return DotName.createSimple(LambdaClient.class.getName());
    }

    @Override
    protected DotName asyncClientName() {
        return DotName.createSimple(LambdaAsyncClient.class.getName());
    }

    @Override
    protected String builtinInterceptorsPath() {
        return "software/amazon/awssdk/services/lambda/execution.interceptors";
    }

    @BuildStep
    AdditionalBeanBuildItem producer() {
        return AdditionalBeanBuildItem.unremovableOf(LambdaProducer.class);
    }

    @BuildStep
    void setup(
            final BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport,
            final BuildProducer<FeatureBuildItem> feature,
            final BuildProducer<AmazonClientInterceptorsPathBuildItem> interceptors) {

        setupExtension(extensionSslNativeSupport, feature, interceptors);
    }

    @BuildStep
    void discover(
            final BeanRegistrationPhaseBuildItem beanRegistrationPhase,
            final BuildProducer<RequireAmazonClientBuildItem> requireClientProducer) {

        discoverClient(beanRegistrationPhase, requireClientProducer);
    }

    @BuildStep
    void setupClient(
            final List<RequireAmazonClientBuildItem> clientRequirements,
            final BuildProducer<AmazonClientBuildItem> clientProducer) {

        setupClient(
                clientRequirements,
                clientProducer,
                buildTimeConfig.sdk(),
                buildTimeConfig.syncClient(),
                buildTimeConfig.asyncClient());
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonApacheHttpServicePresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupApacheSyncTransport(
            final List<AmazonClientBuildItem> amazonClients,
            final LambdaRecorder recorder,
            final AmazonClientApacheTransportRecorder transportRecorder,
            final BuildProducer<AmazonClientSyncTransportBuildItem> syncTransports) {

        createApacheSyncTransportBuilder(
                amazonClients,
                transportRecorder,
                buildTimeConfig.syncClient(),
                recorder.getSyncConfig(),
                syncTransports);
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonUrlConnectionHttpServicePresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupUrlConnectionSyncTransport(
            final List<AmazonClientBuildItem> amazonClients,
            final LambdaRecorder recorder,
            final AmazonClientUrlConnectionTransportRecorder transportRecorder,
            final BuildProducer<AmazonClientSyncTransportBuildItem> syncTransports) {

        createUrlConnectionSyncTransportBuilder(
                amazonClients,
                transportRecorder,
                buildTimeConfig.syncClient(),
                recorder.getSyncConfig(),
                syncTransports);
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonNettyHttpServicePresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupNettyAsyncTransport(
            final List<AmazonClientBuildItem> amazonClients,
            final LambdaRecorder recorder,
            final AmazonClientNettyTransportRecorder transportRecorder,
            final LambdaConfig runtimeConfig,
            final BuildProducer<AmazonClientAsyncTransportBuildItem> asyncTransports,
            final EventLoopGroupBuildItem eventLoopSupplier) {

        createNettyAsyncTransportBuilder(
                amazonClients,
                transportRecorder,
                buildTimeConfig.asyncClient(),
                recorder.getAsyncConfig(),
                asyncTransports,
                eventLoopSupplier.getMainEventLoopGroup());
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonAwsCrtHttpServicePresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupAwsCrtAsyncTransport(final List<AmazonClientBuildItem> amazonClients, final LambdaRecorder recorder,
            final AmazonClientAwsCrtTransportRecorder transportRecorder,
            final BuildProducer<AmazonClientAsyncTransportBuildItem> asyncTransports) {

        createAwsCrtAsyncTransportBuilder(amazonClients,
                transportRecorder,
                buildTimeConfig.asyncClient(),
                recorder.getAsyncConfig(),
                asyncTransports);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void createClientBuilders(final LambdaRecorder recorder,
            final AmazonClientCommonRecorder commonRecorder,
            final List<AmazonClientSyncTransportBuildItem> syncTransports,
            final List<AmazonClientAsyncTransportBuildItem> asyncTransports,
            final BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            final BuildProducer<AmazonClientSyncResultBuildItem> clientSync,
            final BuildProducer<AmazonClientAsyncResultBuildItem> clientAsync,
            final ExecutorBuildItem executorBuildItem) {

        createClientBuilders(recorder,
                commonRecorder,
                buildTimeConfig,
                syncTransports,
                asyncTransports,
                LambdaClientBuilder.class,
                LambdaAsyncClientBuilder.class,
                null,
                syntheticBeans,
                clientSync,
                clientAsync,
                executorBuildItem);
    }
}
