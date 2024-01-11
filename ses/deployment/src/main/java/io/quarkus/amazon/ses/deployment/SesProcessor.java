package io.quarkus.amazon.ses.deployment;

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
import io.quarkus.amazon.common.runtime.AmazonClientOpenTelemetryRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientUrlConnectionTransportRecorder;
import io.quarkus.amazon.ses.runtime.SesBuildTimeConfig;
import io.quarkus.amazon.ses.runtime.SesClientProducer;
import io.quarkus.amazon.ses.runtime.SesConfig;
import io.quarkus.amazon.ses.runtime.SesRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import software.amazon.awssdk.services.ses.SesAsyncClient;
import software.amazon.awssdk.services.ses.SesAsyncClientBuilder;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.SesClientBuilder;

public class SesProcessor extends AbstractAmazonServiceProcessor {

    private static final String AMAZON_SES = "amazon-ses";

    SesBuildTimeConfig buildTimeConfig;

    @Override
    protected String amazonServiceClientName() {
        return AMAZON_SES;
    }

    @Override
    protected String configName() {
        return "ses";
    }

    @Override
    protected DotName syncClientName() {
        return DotName.createSimple(SesClient.class.getName());
    }

    @Override
    protected DotName asyncClientName() {
        return DotName.createSimple(SesAsyncClient.class.getName());
    }

    @Override
    protected String builtinInterceptorsPath() {
        return "software/amazon/awssdk/services/ses/execution.interceptors";
    }

    @BuildStep
    AdditionalBeanBuildItem producer() {
        return AdditionalBeanBuildItem.unremovableOf(SesClientProducer.class);
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

        setupClient(clientRequirements, clientProducer, buildTimeConfig.sdk(), buildTimeConfig.syncClient(),
                buildTimeConfig.asyncClient());
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonApacheHttpServicePresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupApacheSyncTransport(List<AmazonClientBuildItem> amazonClients, SesRecorder recorder,
            AmazonClientApacheTransportRecorder transportRecorder,
            SesConfig runtimeConfig, BuildProducer<AmazonClientSyncTransportBuildItem> syncTransports) {

        createApacheSyncTransportBuilder(amazonClients,
                transportRecorder,
                buildTimeConfig.syncClient(),
                recorder.getSyncConfig(),
                syncTransports);
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonAwsCrtHttpServicePresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupAwsCrtSyncTransport(List<AmazonClientBuildItem> amazonClients, SesRecorder recorder,
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
    void setupUrlConnectionSyncTransport(List<AmazonClientBuildItem> amazonClients, SesRecorder recorder,
            AmazonClientUrlConnectionTransportRecorder transportRecorder,
            SesConfig runtimeConfig, BuildProducer<AmazonClientSyncTransportBuildItem> syncTransports) {

        createUrlConnectionSyncTransportBuilder(amazonClients,
                transportRecorder,
                buildTimeConfig.syncClient(),
                recorder.getSyncConfig(),
                syncTransports);
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonNettyHttpServicePresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupNettyAsyncTransport(List<AmazonClientBuildItem> amazonClients, SesRecorder recorder,
            AmazonClientNettyTransportRecorder transportRecorder,
            SesConfig runtimeConfig, BuildProducer<AmazonClientAsyncTransportBuildItem> asyncTransports,
            EventLoopGroupBuildItem eventLoopSupplier) {

        createNettyAsyncTransportBuilder(amazonClients,
                transportRecorder,
                buildTimeConfig.asyncClient(),
                recorder.getAsyncConfig(),
                asyncTransports, eventLoopSupplier.getMainEventLoopGroup());
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonAwsCrtHttpServicePresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupAwsCrtAsyncTransport(List<AmazonClientBuildItem> amazonClients, SesRecorder recorder,
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
    void createClientBuilders(SesRecorder recorder,
            Capabilities capabilities,
            AmazonClientCommonRecorder commonRecorder,
            AmazonClientOpenTelemetryRecorder otelRecorder,
            SesConfig runtimeConfig,
            List<AmazonClientSyncTransportBuildItem> syncTransports,
            List<AmazonClientAsyncTransportBuildItem> asyncTransports,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<AmazonClientSyncResultBuildItem> clientSync,
            BuildProducer<AmazonClientAsyncResultBuildItem> clientAsync,
            ExecutorBuildItem executorBuildItem) {

        createClientBuilders(capabilities,
                recorder,
                commonRecorder,
                otelRecorder,
                buildTimeConfig,
                syncTransports,
                asyncTransports,
                SesClientBuilder.class,
                SesAsyncClientBuilder.class,
                null,
                syntheticBeans,
                clientSync,
                clientAsync,
                executorBuildItem);
    }
}
