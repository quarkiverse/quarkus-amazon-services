package io.quarkus.amazon.secretsmanager.config.deployment;

import java.util.List;
import java.util.Optional;

import org.jboss.jandex.DotName;

import io.quarkus.amazon.common.deployment.AbstractAmazonServiceProcessor;
import io.quarkus.amazon.common.deployment.AmazonClientInterceptorsPathBuildItem;
import io.quarkus.amazon.common.deployment.AmazonHttpClients;
import io.quarkus.amazon.common.deployment.RequireAmazonClientBuildItem;
import io.quarkus.amazon.common.runtime.AbstractAmazonClientTransportRecorder;
import io.quarkus.amazon.common.runtime.AsyncHttpClientBuildTimeConfig.AsyncClientType;
import io.quarkus.amazon.common.runtime.SyncHttpClientBuildTimeConfig;
import io.quarkus.amazon.secretsmanager.config.runtime.SecretsManagerConfigBootstrapRecorder;
import io.quarkus.amazon.secretsmanager.config.runtime.SecretsManagerConfigBuildTimeConfig;
import io.quarkus.amazon.secretsmanager.config.runtime.SecretsManagerConfigSourceAsyncFactoryBuilder;
import io.quarkus.amazon.secretsmanager.config.runtime.SecretsManagerConfigSourceSyncFactoryBuilder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
import io.quarkus.runtime.RuntimeValue;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

public class SecretsManagerConfigProcessor extends AbstractAmazonServiceProcessor {

    private static final String AMAZON_SECRETS_MANAGER_CONFIG = "amazon-secretsmanager-config";

    private static final DotName SYNC_FACTORY = DotName.createSimple(SecretsManagerConfigSourceSyncFactoryBuilder.class);
    private static final DotName ASYNC_FACTORY = DotName.createSimple(SecretsManagerConfigSourceAsyncFactoryBuilder.class);

    SecretsManagerConfigBuildTimeConfig buildTimeConfig;

    @Override
    protected String amazonServiceClientName() {
        return AMAZON_SECRETS_MANAGER_CONFIG;
    }

    @Override
    protected String configName() {
        return "secretsmanager-config";
    }

    @Override
    protected DotName syncClientName() {
        return DotName.createSimple(SecretsManagerClient.class.getName());
    }

    @Override
    protected DotName asyncClientName() {
        return DotName.createSimple(SecretsManagerAsyncClient.class.getName());
    }

    @Override
    protected String builtinInterceptorsPath() {
        return "software/amazon/awssdk/services/secretsmanager/execution.interceptors";
    }

    @BuildStep
    void setup(
            BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<AmazonClientInterceptorsPathBuildItem> interceptors) {

        setupExtension(extensionSslNativeSupport, feature, interceptors);
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonNettyHttpServicePresent.class)
    @Record(ExecutionTime.STATIC_INIT)
    public void initBoostrapNettyAsyncTranspor(SecretsManagerConfigBootstrapRecorder recorder,
            BuildProducer<SecretsManagerConfigTransportBuildItem> transport,
            BuildProducer<RequireAmazonClientBuildItem> requireClientProducer) {

        if (buildTimeConfig.asyncClient().type() != AsyncClientType.NETTY) {
            return;
        }

        RuntimeValue<AbstractAmazonClientTransportRecorder> transportRecorder = recorder
                .createNettyTransportRecorder();

        transport.produce(new SecretsManagerConfigTransportBuildItem(Optional.empty(), Optional.of(transportRecorder)));
        requireClientProducer
                .produce(new RequireAmazonClientBuildItem(Optional.empty(), Optional.of(asyncClientName())));
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonAwsCrtHttpServicePresent.class)
    @Record(ExecutionTime.STATIC_INIT)
    public void initBoostrapCrtAsyncTranspor(SecretsManagerConfigBootstrapRecorder recorder,
            BuildProducer<SecretsManagerConfigTransportBuildItem> transport,
            BuildProducer<RequireAmazonClientBuildItem> requireClientProducer) {

        if (buildTimeConfig.asyncClient().type() != AsyncClientType.AWS_CRT) {
            return;
        }

        RuntimeValue<AbstractAmazonClientTransportRecorder> transportRecorder = recorder
                .createAwsCrtTransportRecorder();

        transport.produce(new SecretsManagerConfigTransportBuildItem(Optional.empty(), Optional.of(transportRecorder)));
        requireClientProducer
                .produce(new RequireAmazonClientBuildItem(Optional.empty(), Optional.of(asyncClientName())));
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonApacheHttpServicePresent.class)
    @Record(ExecutionTime.STATIC_INIT)
    public void initBoostrapApacheSyncTransport(SecretsManagerConfigBootstrapRecorder recorder,
            BuildProducer<SecretsManagerConfigTransportBuildItem> transport,
            BuildProducer<RequireAmazonClientBuildItem> requireClientProducer) {

        if (buildTimeConfig.syncClient().type() != SyncHttpClientBuildTimeConfig.SyncClientType.APACHE) {
            return;
        }

        RuntimeValue<AbstractAmazonClientTransportRecorder> transportRecorder = recorder
                .createApacheTransportRecorder();

        transport.produce(new SecretsManagerConfigTransportBuildItem(Optional.of(transportRecorder), Optional.empty()));
        requireClientProducer
                .produce(new RequireAmazonClientBuildItem(Optional.of(syncClientName()), Optional.empty()));
    }

    @BuildStep(onlyIf = AmazonHttpClients.IsAmazonUrlConnectionHttpServicePresent.class)
    @Record(ExecutionTime.STATIC_INIT)
    public void initBoostrapUrlConnectionSyncTransport(SecretsManagerConfigBootstrapRecorder recorder,
            BuildProducer<SecretsManagerConfigTransportBuildItem> transport,
            BuildProducer<RequireAmazonClientBuildItem> requireClientProducer) {

        if (buildTimeConfig.syncClient().type() != SyncHttpClientBuildTimeConfig.SyncClientType.URL) {
            return;
        }

        RuntimeValue<AbstractAmazonClientTransportRecorder> transportRecorder = recorder
                .createUrlConnectionTransportRecorder();

        transport.produce(new SecretsManagerConfigTransportBuildItem(Optional.of(transportRecorder), Optional.empty()));
        requireClientProducer
                .produce(new RequireAmazonClientBuildItem(Optional.of(syncClientName()), Optional.empty()));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void setupConfigSource(SecretsManagerConfigBootstrapRecorder recorder,
            List<SecretsManagerConfigTransportBuildItem> transportRecorders,
            ExecutorBuildItem executor,
            BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilder) {

        if (transportRecorders.isEmpty()) {
            return;
        }

        Optional<RuntimeValue<AbstractAmazonClientTransportRecorder>> syncTransportRecorder = Optional.empty();
        Optional<RuntimeValue<AbstractAmazonClientTransportRecorder>> asyncTransportRecorder = Optional.empty();
        for (SecretsManagerConfigTransportBuildItem transportRecorder : transportRecorders) {

            if (transportRecorder.getSyncTransporter().isPresent()) {
                syncTransportRecorder = transportRecorder.getSyncTransporter();
            }
            if (transportRecorder.getAsyncTransporter().isPresent()) {
                asyncTransportRecorder = transportRecorder.getAsyncTransporter();
            }
        }

        if (syncTransportRecorder.isPresent() || asyncTransportRecorder.isPresent()) {

            RuntimeValue<AbstractAmazonClientTransportRecorder> transportRecorder = syncTransportRecorder
                    .orElse(asyncTransportRecorder.get());

            recorder.configure(transportRecorder, buildTimeConfig.sdk().interceptors());

            if (syncTransportRecorder.isPresent()) {
                runTimeConfigBuilder
                        .produce(new RunTimeConfigBuilderBuildItem(SYNC_FACTORY.toString()));
            } else {
                runTimeConfigBuilder
                        .produce(new RunTimeConfigBuilderBuildItem(ASYNC_FACTORY.toString()));
            }
        }
    }
}
