package io.quarkus.amazon.common.deployment;

import static io.quarkus.amazon.common.deployment.ClientDeploymentUtil.getNamedClientInjection;
import static io.quarkus.amazon.common.deployment.ClientDeploymentUtil.injectionPointAnnotationsBuilder;
import static io.quarkus.amazon.common.deployment.ClientDeploymentUtil.namedBuilder;
import static io.quarkus.amazon.common.deployment.ClientDeploymentUtil.namedClient;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import io.netty.channel.EventLoopGroup;
import io.opentelemetry.instrumentation.awssdk.v2_2.AwsSdkTelemetry;
import io.quarkus.amazon.common.runtime.AmazonClientApacheTransportRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientAwsCrtTransportRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientCommonRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientNettyTransportRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientOpenTelemetryRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientRecorder;
import io.quarkus.amazon.common.runtime.AmazonClientUrlConnectionTransportRecorder;
import io.quarkus.amazon.common.runtime.AsyncHttpClientBuildTimeConfig;
import io.quarkus.amazon.common.runtime.AsyncHttpClientConfig;
import io.quarkus.amazon.common.runtime.HasAmazonClientRuntimeConfig;
import io.quarkus.amazon.common.runtime.HasSdkBuildTimeConfig;
import io.quarkus.amazon.common.runtime.SdkAutoCloseableDestroyer;
import io.quarkus.amazon.common.runtime.SdkBuildTimeConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientBuildTimeConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.runtime.RuntimeValue;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.presigner.SdkPresigner;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

abstract public class AbstractAmazonServiceProcessor {

    abstract protected String amazonServiceClientName();

    abstract protected String configName();

    abstract protected DotName syncClientName();

    abstract protected DotName asyncClientName();

    protected DotName presignerClientName() {
        return null;
    }

    abstract protected String builtinInterceptorsPath();

    protected void discoverClientInjectionPointsInternal(BeanRegistrationPhaseBuildItem beanRegistrationPhase,
            BuildProducer<RequireAmazonClientInjectionBuildItem> requireClientInjectionProducer) {

        // Discover all clients injections
        for (InjectionPointInfo injectionPoint : beanRegistrationPhase.getInjectionPoints()) {

            Type injectedType = getInjectedType(injectionPoint);

            if (syncClientName().equals(injectedType.name())) {
                requireClientInjectionProducer
                        .produce(new RequireAmazonClientInjectionBuildItem(syncClientName(),
                                getNamedClientInjection(injectionPoint)));
            }
            if (asyncClientName().equals(injectedType.name())) {
                requireClientInjectionProducer
                        .produce(new RequireAmazonClientInjectionBuildItem(asyncClientName(),
                                getNamedClientInjection(injectionPoint)));
            }
            if (presignerClientName() != null && presignerClientName().equals(injectedType.name())) {
                requireClientInjectionProducer
                        .produce(new RequireAmazonClientInjectionBuildItem(presignerClientName(),
                                getNamedClientInjection(injectionPoint)));
            }
        }
    }

    @BuildStep
    protected void discoverClient(
            List<RequireAmazonClientInjectionBuildItem> amazonClientInjectionPoints,
            BuildProducer<RequireAmazonClientBuildItem> requireClientProducer) {
        Optional<DotName> syncClassName = Optional.empty();
        Optional<DotName> asyncClassName = Optional.empty();

        for (RequireAmazonClientInjectionBuildItem requireInjectionPoint : amazonClientInjectionPoints) {
            if (syncClientName().equals(requireInjectionPoint.getClassName())) {
                syncClassName = Optional.of(syncClientName());
            }
            if (asyncClientName().equals(requireInjectionPoint.getClassName())) {
                asyncClassName = Optional.of(asyncClientName());
            }
        }

        if (syncClassName.isPresent() || asyncClassName.isPresent()) {
            requireClientProducer.produce(new RequireAmazonClientBuildItem(syncClassName, asyncClassName));
        }
    }

    protected void discoverTelemetry(BuildProducer<RequireAmazonTelemetryBuildItem> telemetryProducer,
            SdkBuildTimeConfig buildTimeSdkConfig) {
        if (buildTimeSdkConfig.telemetry().orElse(false)) {
            telemetryProducer.produce(new RequireAmazonTelemetryBuildItem(configName()));
        }
    }

    protected void setupClient(List<RequireAmazonClientBuildItem> clientRequirements,
            BuildProducer<RequireAmazonClientTransportBuilderBuildItem> clientProducer,
            SdkBuildTimeConfig buildTimeSdkConfig,
            SyncHttpClientBuildTimeConfig buildTimeSyncConfig,
            AsyncHttpClientBuildTimeConfig buildTimeAsyncConfig) {

        Optional<DotName> syncClassName = Optional.empty();
        Optional<DotName> asyncClassName = Optional.empty();

        for (RequireAmazonClientBuildItem clientRequirement : clientRequirements) {

            if (clientRequirement.getSyncClassName().filter(syncClientName()::equals).isPresent()) {
                syncClassName = Optional.of(syncClientName());
            }
            if (clientRequirement.getAsyncClassName().filter(asyncClientName()::equals).isPresent()) {
                asyncClassName = Optional.of(asyncClientName());
            }
        }
        if (syncClassName.isPresent() || asyncClassName.isPresent()) {
            clientProducer.produce(new RequireAmazonClientTransportBuilderBuildItem(syncClassName, asyncClassName, configName(),
                    buildTimeSdkConfig, buildTimeSyncConfig, buildTimeAsyncConfig));
        }
    }

    protected void setupExtension(
            BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<AmazonClientInterceptorsPathBuildItem> interceptors) {

        feature.produce(new FeatureBuildItem(amazonServiceClientName()));
        extensionSslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(amazonServiceClientName()));
        interceptors.produce(new AmazonClientInterceptorsPathBuildItem(builtinInterceptorsPath()));
    }

    protected void createApacheSyncTransportBuilder(List<RequireAmazonClientTransportBuilderBuildItem> amazonClients,
            AmazonClientApacheTransportRecorder recorder,
            SyncHttpClientBuildTimeConfig buildSyncConfig,
            RuntimeValue<SyncHttpClientConfig> syncConfig,
            BuildProducer<AmazonClientSyncTransportBuildItem> clientSyncTransports) {

        Optional<RequireAmazonClientTransportBuilderBuildItem> matchingClientBuildItem = amazonClients.stream()
                .filter(c -> c.getAwsClientName().equals(configName()))
                .findAny();

        matchingClientBuildItem.ifPresent(client -> {
            if (!client.getSyncClassName().isPresent()) {
                return;
            }
            if (buildSyncConfig.type() != SyncHttpClientBuildTimeConfig.SyncClientType.APACHE) {
                return;
            }

            clientSyncTransports.produce(
                    new AmazonClientSyncTransportBuildItem(
                            client.getAwsClientName(),
                            client.getSyncClassName().get(),
                            recorder.configureSync(configName(), syncConfig)));
        });
    }

    protected void createAwsCrtSyncTransportBuilder(List<RequireAmazonClientTransportBuilderBuildItem> amazonClients,
            AmazonClientAwsCrtTransportRecorder recorder,
            SyncHttpClientBuildTimeConfig buildSyncConfig,
            RuntimeValue<SyncHttpClientConfig> syncConfig,
            BuildProducer<AmazonClientSyncTransportBuildItem> clientSyncTransports) {

        Optional<RequireAmazonClientTransportBuilderBuildItem> matchingClientBuildItem = amazonClients.stream()
                .filter(c -> c.getAwsClientName().equals(configName()))
                .findAny();

        matchingClientBuildItem.ifPresent(client -> {
            if (!client.getSyncClassName().isPresent()) {
                return;
            }
            if (buildSyncConfig.type() != SyncHttpClientBuildTimeConfig.SyncClientType.AWS_CRT) {
                return;
            }

            clientSyncTransports.produce(
                    new AmazonClientSyncTransportBuildItem(
                            client.getAwsClientName(),
                            client.getSyncClassName().get(),
                            recorder.configureSync(configName(), syncConfig)));
        });
    }

    protected void createUrlConnectionSyncTransportBuilder(List<RequireAmazonClientTransportBuilderBuildItem> amazonClients,
            AmazonClientUrlConnectionTransportRecorder recorder,
            SyncHttpClientBuildTimeConfig buildSyncConfig,
            RuntimeValue<SyncHttpClientConfig> syncConfig,
            BuildProducer<AmazonClientSyncTransportBuildItem> clientSyncTransports) {

        Optional<RequireAmazonClientTransportBuilderBuildItem> matchingClientBuildItem = amazonClients.stream()
                .filter(c -> c.getAwsClientName().equals(configName()))
                .findAny();

        matchingClientBuildItem.ifPresent(client -> {
            if (!client.getSyncClassName().isPresent()) {
                return;
            }
            if (buildSyncConfig.type() != SyncHttpClientBuildTimeConfig.SyncClientType.URL) {
                return;
            }

            clientSyncTransports.produce(
                    new AmazonClientSyncTransportBuildItem(
                            client.getAwsClientName(),
                            client.getSyncClassName().get(),
                            recorder.configureSync(configName(), syncConfig)));
        });
    }

    protected void createNettyAsyncTransportBuilder(List<RequireAmazonClientTransportBuilderBuildItem> amazonClients,
            AmazonClientNettyTransportRecorder recorder,
            AsyncHttpClientBuildTimeConfig buildAsyncConfig,
            RuntimeValue<AsyncHttpClientConfig> asyncConfig,
            BuildProducer<AmazonClientAsyncTransportBuildItem> clientAsyncTransports,
            Supplier<EventLoopGroup> eventLoopSupplier) {

        Optional<RequireAmazonClientTransportBuilderBuildItem> matchingClientBuildItem = amazonClients.stream()
                .filter(c -> c.getAwsClientName().equals(configName()))
                .findAny();

        matchingClientBuildItem.ifPresent(client -> {
            if (!client.getAsyncClassName().isPresent()) {
                return;
            }
            if (buildAsyncConfig.type() != AsyncHttpClientBuildTimeConfig.AsyncClientType.NETTY) {
                return;
            }

            clientAsyncTransports.produce(
                    new AmazonClientAsyncTransportBuildItem(
                            client.getAwsClientName(),
                            client.getAsyncClassName().get(),
                            recorder.configureNettyAsync(recorder.configureAsync(configName(), asyncConfig), eventLoopSupplier,
                                    asyncConfig)));
        });
    }

    protected void createAwsCrtAsyncTransportBuilder(List<RequireAmazonClientTransportBuilderBuildItem> amazonClients,
            AmazonClientAwsCrtTransportRecorder recorder,
            AsyncHttpClientBuildTimeConfig buildAsyncConfig,
            RuntimeValue<AsyncHttpClientConfig> asyncConfig,
            BuildProducer<AmazonClientAsyncTransportBuildItem> clientAsyncTransports) {

        Optional<RequireAmazonClientTransportBuilderBuildItem> matchingClientBuildItem = amazonClients.stream()
                .filter(c -> c.getAwsClientName().equals(configName()))
                .findAny();

        matchingClientBuildItem.ifPresent(client -> {
            if (!client.getAsyncClassName().isPresent()) {
                return;
            }
            if (buildAsyncConfig.type() != AsyncHttpClientBuildTimeConfig.AsyncClientType.AWS_CRT) {
                return;
            }

            clientAsyncTransports.produce(
                    new AmazonClientAsyncTransportBuildItem(
                            client.getAwsClientName(),
                            client.getAsyncClassName().get(),
                            recorder.configureAsync(configName(), asyncConfig)));
        });
    }

    protected void createClientBuilders(
            AmazonClientRecorder recorder,
            AmazonClientCommonRecorder commonRecorder,
            AmazonClientOpenTelemetryRecorder otelRecorder,
            HasSdkBuildTimeConfig sdkBuildConfig,
            List<RequireAmazonClientInjectionBuildItem> amazonClientInjections,
            List<RequireAmazonTelemetryBuildItem> amazonRequireTelemtryClients,
            List<AmazonClientSyncTransportBuildItem> syncTransports,
            List<AmazonClientAsyncTransportBuildItem> asyncTransports,
            Class<?> syncClientBuilderClass,
            Class<?> asyncClientBuilderClass,
            Class<?> presignerBuilderClass,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<AmazonClientSyncResultBuildItem> clientSync,
            BuildProducer<AmazonClientAsyncResultBuildItem> clientAsync,
            LaunchModeBuildItem launchModeBuildItem,
            ExecutorBuildItem executorBuildItem) {

        Supplier<RuntimeValue<SdkPresigner.Builder>> presignerBuilderSupplier = null;
        if (presignerBuilderClass != null) {
            presignerBuilderSupplier = () -> recorder.createPresignerBuilder();
        }

        createClientBuilders(
                commonRecorder,
                otelRecorder,
                recorder.getAmazonClientsConfig(),
                sdkBuildConfig,
                amazonClientInjections,
                amazonRequireTelemtryClients,
                syncTransports,
                asyncTransports,
                syncClientBuilderClass,
                (syncTransport) -> recorder.createSyncBuilder(syncTransport),
                asyncClientBuilderClass,
                (asyncTransport) -> recorder.createAsyncBuilder(asyncTransport, launchModeBuildItem.getLaunchMode(),
                        executorBuildItem.getExecutorProxy()),
                presignerBuilderClass,
                presignerBuilderSupplier,
                syntheticBeans,
                clientSync,
                clientAsync,
                launchModeBuildItem,
                executorBuildItem);
    }

    private void createClientBuilders(
            AmazonClientCommonRecorder recorder,
            AmazonClientOpenTelemetryRecorder otelRecorder,
            RuntimeValue<HasAmazonClientRuntimeConfig> amazonClientConfigRuntime,
            HasSdkBuildTimeConfig sdkBuildConfig,
            List<RequireAmazonClientInjectionBuildItem> amazonClientInjections,
            List<RequireAmazonTelemetryBuildItem> amazonRequireTelemtryClients,
            List<AmazonClientSyncTransportBuildItem> amazonClientSyncTransports,
            List<AmazonClientAsyncTransportBuildItem> amazonClientAsyncTransports,
            Class<?> syncClientBuilderClass,
            Function<RuntimeValue<SdkHttpClient.Builder>, RuntimeValue<AwsClientBuilder>> syncClientBuilderFunction,
            Class<?> asyncClientBuilderClass,
            Function<RuntimeValue<SdkAsyncHttpClient.Builder>, RuntimeValue<AwsClientBuilder>> asyncClientBuilderFunction,
            Class<?> presignerBuilderClass,
            Supplier<RuntimeValue<SdkPresigner.Builder>> presignerBuilderSupplier,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<AmazonClientSyncResultBuildItem> clientSync,
            BuildProducer<AmazonClientAsyncResultBuildItem> clientAsync,
            LaunchModeBuildItem launchModeBuildItem,
            ExecutorBuildItem executorBuildItem) {
        String configName = configName();

        Optional<RuntimeValue<SdkHttpClient.Builder>> syncSdkHttpClientBuilder = amazonClientSyncTransports.stream()
                .filter(c -> configName.equals(c.getAwsClientName()))
                .map(c -> c.getClientBuilder())
                .findFirst();
        Optional<RuntimeValue<SdkAsyncHttpClient.Builder>> asyncSdkAsyncHttpClientBuilder = amazonClientAsyncTransports
                .stream()
                .filter(c -> configName.equals(c.getAwsClientName()))
                .map(c -> c.getClientBuilder())
                .findFirst();

        boolean addOpenTelemetry = amazonRequireTelemtryClients
                .stream()
                .anyMatch(c -> configName.equals(c.getConfigName()));

        if (!syncSdkHttpClientBuilder.isPresent() && !asyncSdkAsyncHttpClientBuilder.isPresent()
                && presignerBuilderSupplier == null) {
            return;
        }

        // requiring named clients can originate from multiple sources and we may have duplicates
        Collection<String> syncClientNames = amazonClientInjections.stream()
                .filter(c -> syncClientName().equals(c.getClassName()))
                .map(c -> c.getName())
                .distinct()
                .collect(Collectors.toSet());

        Collection<String> asyncClientNames = amazonClientInjections.stream()
                .filter(c -> asyncClientName().equals(c.getClassName()))
                .map(c -> c.getName())
                .distinct()
                .collect(Collectors.toSet());

        Collection<String> presignerClientNames = amazonClientInjections.stream()
                .filter(c -> presignerClientName() != null && presignerClientName().equals(c.getClassName()))
                .map(c -> c.getName())
                .distinct()
                .collect(Collectors.toSet());

        ScheduledExecutorService sharedExecutorService = executorBuildItem.getExecutorProxy();

        if (syncSdkHttpClientBuilder.isPresent() && !syncClientNames.isEmpty()) {
            for (String clientName : syncClientNames) {
                RuntimeValue<AwsClientBuilder> syncClientBuilder = syncClientBuilderFunction
                        .apply(syncSdkHttpClientBuilder.get());

                syncClientBuilder = recorder.configure(syncClientBuilder, amazonClientConfigRuntime,
                        sdkBuildConfig, sharedExecutorService, configName(), clientName);
                if (addOpenTelemetry) {
                    syntheticBeans.produce(namedBuilder(SyntheticBeanBuildItem.configure(syncClientBuilderClass), clientName)
                            .unremovable()
                            .defaultBean()
                            .setRuntimeInit()
                            .scope(ApplicationScoped.class)
                            .createWith(otelRecorder.configureSync(syncClientBuilder))
                            .addInjectionPoint(ClassType.create(AwsSdkTelemetry.class)).done());
                } else {
                    syntheticBeans.produce(namedBuilder(SyntheticBeanBuildItem.configure(syncClientBuilderClass), clientName)
                            .unremovable()
                            .defaultBean()
                            .setRuntimeInit()
                            .scope(ApplicationScoped.class)
                            .runtimeValue(syncClientBuilder)
                            .done());
                }
                syntheticBeans
                        .produce(
                                namedClient(SyntheticBeanBuildItem.configure(syncClientName()), clientName)
                                        .unremovable()
                                        .defaultBean()
                                        .setRuntimeInit()
                                        .scope(ApplicationScoped.class)
                                        .createWith(recorder.build(syncClientBuilderClass, clientName))
                                        .addInjectionPoint(ClassType.create(syncClientBuilderClass),
                                                injectionPointAnnotationsBuilder(clientName))
                                        .destroyer(SdkAutoCloseableDestroyer.class)
                                        .done());

                clientSync.produce(new AmazonClientSyncResultBuildItem(configName, clientName));
            }
        }
        if (asyncSdkAsyncHttpClientBuilder.isPresent() && !asyncClientNames.isEmpty()) {
            for (String clientName : asyncClientNames) {
                RuntimeValue<AwsClientBuilder> asyncClientBuilder = asyncClientBuilderFunction
                        .apply(asyncSdkAsyncHttpClientBuilder.get());

                asyncClientBuilder = recorder.configure(asyncClientBuilder, amazonClientConfigRuntime,
                        sdkBuildConfig, sharedExecutorService, configName(), clientName);
                if (addOpenTelemetry) {
                    syntheticBeans.produce(namedBuilder(SyntheticBeanBuildItem.configure(asyncClientBuilderClass), clientName)
                            .unremovable()
                            .defaultBean()
                            .setRuntimeInit()
                            .scope(ApplicationScoped.class)
                            .createWith(otelRecorder.configureAsync(asyncClientBuilder))
                            .addInjectionPoint(ClassType.create(AwsSdkTelemetry.class)).done());
                } else {
                    syntheticBeans.produce(namedBuilder(SyntheticBeanBuildItem.configure(asyncClientBuilderClass), clientName)
                            .unremovable()
                            .defaultBean()
                            .setRuntimeInit()
                            .scope(ApplicationScoped.class)
                            .runtimeValue(asyncClientBuilder)
                            .done());
                }
                syntheticBeans.produce(
                        namedClient(SyntheticBeanBuildItem.configure(asyncClientName()), clientName)
                                .unremovable()
                                .defaultBean()
                                .setRuntimeInit()
                                .scope(ApplicationScoped.class)
                                .createWith(recorder.build(asyncClientBuilderClass, clientName))
                                .addInjectionPoint(ClassType.create(asyncClientBuilderClass),
                                        injectionPointAnnotationsBuilder(clientName))
                                .destroyer(SdkAutoCloseableDestroyer.class)
                                .done());

                clientAsync.produce(new AmazonClientAsyncResultBuildItem(configName, clientName));
            }
        }
        if (presignerBuilderSupplier != null && !presignerClientNames.isEmpty()) {
            for (String clientName : presignerClientNames) {
                RuntimeValue<SdkPresigner.Builder> presignerBuilder = presignerBuilderSupplier.get();

                presignerBuilder = recorder.configurePresigner(presignerBuilder, amazonClientConfigRuntime,
                        configName(), clientName);
                syntheticBeans.produce(
                        namedBuilder(SyntheticBeanBuildItem.configure(presignerBuilderClass), clientName)
                                .unremovable()
                                .defaultBean()
                                .setRuntimeInit()
                                .scope(ApplicationScoped.class)
                                .runtimeValue(presignerBuilder)
                                .done());
                syntheticBeans.produce(
                        namedClient(SyntheticBeanBuildItem.configure(presignerClientName()), clientName)
                                .unremovable()
                                .defaultBean()
                                .setRuntimeInit()
                                .scope(ApplicationScoped.class)
                                .createWith(recorder.buildPresigner(presignerBuilderClass, clientName))
                                .addInjectionPoint(ClassType.create(presignerBuilderClass),
                                        injectionPointAnnotationsBuilder(clientName))
                                .destroyer(SdkAutoCloseableDestroyer.class)
                                .done());
            }
        }
    }

    private Type getInjectedType(InjectionPointInfo injectionPoint) {
        Type requiredType = injectionPoint.getRequiredType();
        Type injectedType = requiredType;

        if (DotNames.INSTANCE.equals(requiredType.name()) && requiredType instanceof ParameterizedType) {
            injectedType = requiredType.asParameterizedType().arguments().get(0);
        }

        return injectedType;
    }
}
