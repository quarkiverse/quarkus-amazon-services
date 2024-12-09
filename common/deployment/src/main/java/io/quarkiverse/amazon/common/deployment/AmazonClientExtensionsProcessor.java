package io.quarkiverse.amazon.common.deployment;

import static io.quarkiverse.amazon.common.deployment.ClientDeploymentUtil.getNamedClientInjection;
import static io.quarkiverse.amazon.common.deployment.ClientDeploymentUtil.injectionPointAnnotationsBuilder;
import static io.quarkiverse.amazon.common.deployment.ClientDeploymentUtil.namedBuilder;
import static io.quarkiverse.amazon.common.deployment.ClientDeploymentUtil.namedClient;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import io.opentelemetry.instrumentation.awssdk.v2_2.AwsSdkTelemetry;
import io.quarkiverse.amazon.common.runtime.AmazonClientBuilderRecorder;
import io.quarkiverse.amazon.common.runtime.AmazonClientCommonRecorder;
import io.quarkiverse.amazon.common.runtime.AmazonClientOpenTelemetryRecorder;
import io.quarkiverse.amazon.common.runtime.HasAmazonClientRuntimeConfig;
import io.quarkiverse.amazon.common.runtime.HasSdkBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.SdkAutoCloseableDestroyer;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.runtime.RuntimeValue;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.awscore.presigner.SdkPresigner;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

public class AmazonClientExtensionsProcessor {

    @BuildStep
    void setup(
            List<AmazonClientExtensionBuildItem> amazonExtensions,
            BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<AmazonClientInterceptorsPathBuildItem> interceptors) {

        amazonExtensions.forEach(extension -> setupExtension(extension, extensionSslNativeSupport, feature, interceptors));
    }

    protected void setupExtension(
            AmazonClientExtensionBuildItem extension,
            BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<AmazonClientInterceptorsPathBuildItem> interceptors) {

        feature.produce(new FeatureBuildItem(extension.getAmazonServiceClientName()));
        extensionSslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(extension.getAmazonServiceClientName()));
        interceptors.produce(new AmazonClientInterceptorsPathBuildItem(extension.getBuiltinInterceptorsPath()));
    }

    @BuildStep
    void discoverClientInjectionPoints(
            List<AmazonClientExtensionBuildItem> amazonExtensions,
            BeanRegistrationPhaseBuildItem beanRegistrationPhase,
            BuildProducer<RequireAmazonClientInjectionBuildItem> requireClientInjectionProducer) {

        amazonExtensions.forEach(extension -> discoverClientInjectionPointsInternal(extension, beanRegistrationPhase,
                requireClientInjectionProducer));
    }

    protected void discoverClientInjectionPointsInternal(
            AmazonClientExtensionBuildItem extension,
            BeanRegistrationPhaseBuildItem beanRegistrationPhase,
            BuildProducer<RequireAmazonClientInjectionBuildItem> requireClientInjectionProducer) {

        // Discover all clients injections
        for (InjectionPointInfo injectionPoint : beanRegistrationPhase.getInjectionPoints()) {

            Type injectedType = getInjectedType(injectionPoint);

            if (extension.getSyncClientName().equals(injectedType.name())) {
                requireClientInjectionProducer
                        .produce(new RequireAmazonClientInjectionBuildItem(extension.getSyncClientName(),
                                getNamedClientInjection(injectionPoint)));
            }
            if (extension.getAsyncClientName().equals(injectedType.name())) {
                requireClientInjectionProducer
                        .produce(new RequireAmazonClientInjectionBuildItem(extension.getAsyncClientName(),
                                getNamedClientInjection(injectionPoint)));
            }
            if (extension.getPresignerClientName() != null && extension.getPresignerClientName().equals(injectedType.name())) {
                requireClientInjectionProducer
                        .produce(new RequireAmazonClientInjectionBuildItem(extension.getPresignerClientName(),
                                getNamedClientInjection(injectionPoint)));
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

    @BuildStep
    void discover(
            List<AmazonClientExtensionBuildItem> amazonExtensions,
            List<RequireAmazonClientInjectionBuildItem> amazonClientInjectionPoints,
            BuildProducer<RequireAmazonClientBuildItem> requireClientProducer) {

        amazonExtensions.forEach(extension -> discoverClient(extension, amazonClientInjectionPoints, requireClientProducer));
    }

    protected void discoverClient(
            AmazonClientExtensionBuildItem extension,
            List<RequireAmazonClientInjectionBuildItem> amazonClientInjectionPoints,
            BuildProducer<RequireAmazonClientBuildItem> requireClientProducer) {
        Optional<DotName> syncClassName = Optional.empty();
        Optional<DotName> asyncClassName = Optional.empty();

        for (RequireAmazonClientInjectionBuildItem requireInjectionPoint : amazonClientInjectionPoints) {
            if (extension.getSyncClientName().equals(requireInjectionPoint.getClassName())) {
                syncClassName = Optional.of(extension.getSyncClientName());
            }
            if (extension.getAsyncClientName().equals(requireInjectionPoint.getClassName())) {
                asyncClassName = Optional.of(extension.getAsyncClientName());
            }
        }

        if (syncClassName.isPresent() || asyncClassName.isPresent()) {
            requireClientProducer.produce(new RequireAmazonClientBuildItem(syncClassName, asyncClassName));
        }
    }

    @BuildStep
    void discoverTelemetry(
            List<AmazonClientExtensionBuildItem> amazonExtensions,
            BuildProducer<RequireAmazonTelemetryBuildItem> telemetryProducer) {

        amazonExtensions.forEach(extension -> discoverTelemetry(extension, telemetryProducer));
    }

    protected void discoverTelemetry(
            AmazonClientExtensionBuildItem extension,
            BuildProducer<RequireAmazonTelemetryBuildItem> telemetryProducer) {
        if (extension.getHasSdkBuildTimeConfig().sdk().telemetry().orElse(false)) {
            telemetryProducer.produce(new RequireAmazonTelemetryBuildItem(extension.getConfigName()));
        }
    }

    @BuildStep
    void setupClient(
            List<AmazonClientExtensionBuildItem> amazonExtensions,
            List<RequireAmazonClientBuildItem> clientRequirements,
            BuildProducer<RequireAmazonClientTransportBuilderBuildItem> clientProducer) {

        amazonExtensions.forEach(extension -> setupClient(extension, clientRequirements, clientProducer));
    }

    protected void setupClient(
            AmazonClientExtensionBuildItem extension,
            List<RequireAmazonClientBuildItem> clientRequirements,
            BuildProducer<RequireAmazonClientTransportBuilderBuildItem> clientProducer) {

        Optional<DotName> syncClassName = Optional.empty();
        Optional<DotName> asyncClassName = Optional.empty();

        for (RequireAmazonClientBuildItem clientRequirement : clientRequirements) {

            if (clientRequirement.getSyncClassName().filter(extension.getSyncClientName()::equals).isPresent()) {
                syncClassName = Optional.of(extension.getSyncClientName());
            }
            if (clientRequirement.getAsyncClassName().filter(extension.getAsyncClientName()::equals).isPresent()) {
                asyncClassName = Optional.of(extension.getAsyncClientName());
            }
        }
        if (syncClassName.isPresent() || asyncClassName.isPresent()) {
            clientProducer.produce(
                    new RequireAmazonClientTransportBuilderBuildItem(syncClassName, asyncClassName, extension.getConfigName(),
                            extension.getHasSdkBuildTimeConfig().sdk(), extension.getBuildSyncConfig(),
                            extension.getBuildAsyncConfig()));
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void createClientBuilders(
            List<AmazonClientExtensionBuildItem> amazonExtensions,
            List<AmazonClientExtensionBuilderInstanceBuildItem> builderInstances,
            AmazonClientCommonRecorder commonRecorder,
            AmazonClientBuilderRecorder builderRecorder,
            List<RequireAmazonClientInjectionBuildItem> amazonClientInjections,
            List<RequireAmazonTelemetryBuildItem> amazonRequireTelemtryClients,
            List<AmazonClientSyncTransportBuildItem> syncTransports,
            List<AmazonClientAsyncTransportBuildItem> asyncTransports,
            BuildProducer<AmazonClientBuilderBuildItem> clientBuilders,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<AmazonClientSyncResultBuildItem> clientSync,
            BuildProducer<AmazonClientAsyncResultBuildItem> clientAsync,
            LaunchModeBuildItem launchModeBuildItem,
            ExecutorBuildItem executorBuildItem) {
        amazonExtensions.forEach(extension -> createClientBuilders(
                extension,
                builderInstances,
                commonRecorder,
                builderRecorder,
                amazonClientInjections,
                amazonRequireTelemtryClients,
                syncTransports,
                asyncTransports,
                clientBuilders,
                syntheticBeans,
                clientSync,
                clientAsync,
                launchModeBuildItem,
                executorBuildItem));
    }

    protected void createClientBuilders(
            AmazonClientExtensionBuildItem extension,
            List<AmazonClientExtensionBuilderInstanceBuildItem> builderInstances,
            AmazonClientCommonRecorder commonRecorder,
            AmazonClientBuilderRecorder builderRecorder,
            List<RequireAmazonClientInjectionBuildItem> amazonClientInjections,
            List<RequireAmazonTelemetryBuildItem> amazonRequireTelemtryClients,
            List<AmazonClientSyncTransportBuildItem> syncTransports,
            List<AmazonClientAsyncTransportBuildItem> asyncTransports,
            BuildProducer<AmazonClientBuilderBuildItem> clientBuilders,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<AmazonClientSyncResultBuildItem> clientSync,
            BuildProducer<AmazonClientAsyncResultBuildItem> clientAsync,
            LaunchModeBuildItem launchModeBuildItem,
            ExecutorBuildItem executorBuildItem) {

        Function<RuntimeValue<SdkPresigner.Builder>, RuntimeValue<SdkPresigner.Builder>> presignerBuilderSupplier = null;
        if (extension.getPresignerBuilderClass() != null) {
            presignerBuilderSupplier = (presignerBuilder) -> presignerBuilder;
        }

        createClientBuilders(
                extension.getConfigName(),
                builderInstances,
                commonRecorder,
                builderRecorder,
                extension.getAmazonClientsConfig(),
                extension.getHasSdkBuildTimeConfig(),
                amazonClientInjections,
                amazonRequireTelemtryClients,
                syncTransports,
                asyncTransports,
                extension.getSyncClientName(),
                extension.getSyncClientBuilderClass(),
                (syncBuilder, syncTransport) -> builderRecorder.createSyncBuilder(syncBuilder, syncTransport),
                extension.getAsyncClientName(),
                extension.getAsyncClientBuilderClass(),
                (asyncBuilder, asyncTransport) -> builderRecorder.createAsyncBuilder(asyncBuilder, asyncTransport,
                        launchModeBuildItem.getLaunchMode(),
                        executorBuildItem.getExecutorProxy(), extension.getAsyncConfig()),
                extension.getPresignerClientName(),
                extension.getPresignerBuilderClass(),
                presignerBuilderSupplier,
                clientBuilders,
                syntheticBeans,
                clientSync,
                clientAsync,
                launchModeBuildItem,
                executorBuildItem);
    }

    private void createClientBuilders(
            String configName,
            List<AmazonClientExtensionBuilderInstanceBuildItem> builderInstances,
            AmazonClientCommonRecorder recorder,
            AmazonClientBuilderRecorder builderRecorder,
            RuntimeValue<HasAmazonClientRuntimeConfig> amazonClientConfigRuntime,
            HasSdkBuildTimeConfig sdkBuildConfig,
            List<RequireAmazonClientInjectionBuildItem> amazonClientInjections,
            List<RequireAmazonTelemetryBuildItem> amazonRequireTelemtryClients,
            List<AmazonClientSyncTransportBuildItem> amazonClientSyncTransports,
            List<AmazonClientAsyncTransportBuildItem> amazonClientAsyncTransports,
            DotName syncClientName,
            Class<?> syncClientBuilderClass,
            BiFunction<RuntimeValue<AwsSyncClientBuilder<?, ?>>, RuntimeValue<SdkHttpClient.Builder>, RuntimeValue<AwsClientBuilder>> syncClientBuilderFunction,
            DotName asyncClientName,
            Class<?> asyncClientBuilderClass,
            BiFunction<RuntimeValue<AwsAsyncClientBuilder<?, ?>>, RuntimeValue<SdkAsyncHttpClient.Builder>, RuntimeValue<AwsClientBuilder>> asyncClientBuilderFunction,
            DotName presignerClientName,
            Class<?> presignerBuilderClass,
            Function<RuntimeValue<SdkPresigner.Builder>, RuntimeValue<SdkPresigner.Builder>> presignerBuilderSupplier,
            BuildProducer<AmazonClientBuilderBuildItem> clientBuilders,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<AmazonClientSyncResultBuildItem> clientSync,
            BuildProducer<AmazonClientAsyncResultBuildItem> clientAsync,
            LaunchModeBuildItem launchModeBuildItem,
            ExecutorBuildItem executorBuildItem) {

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
                .filter(c -> syncClientName.equals(c.getClassName()))
                .map(c -> c.getName())
                .distinct()
                .collect(Collectors.toSet());

        Collection<String> asyncClientNames = amazonClientInjections.stream()
                .filter(c -> asyncClientName.equals(c.getClassName()))
                .map(c -> c.getName())
                .distinct()
                .collect(Collectors.toSet());

        Collection<String> presignerClientNames = amazonClientInjections.stream()
                .filter(c -> presignerClientName != null && presignerClientName.equals(c.getClassName()))
                .map(c -> c.getName())
                .distinct()
                .collect(Collectors.toSet());

        ScheduledExecutorService sharedExecutorService = executorBuildItem.getExecutorProxy();

        if (syncSdkHttpClientBuilder.isPresent() && !syncClientNames.isEmpty()) {
            for (String clientName : syncClientNames) {
                AmazonClientExtensionBuilderInstanceBuildItem builderInstance = builderInstances.stream()
                        .filter(o -> o.getClientBuilderClass().equals(syncClientBuilderClass) &&
                                o.getClientName().equals(clientName))
                        .findFirst().orElseThrow();

                RuntimeValue<AwsClientBuilder> syncClientBuilder = syncClientBuilderFunction
                        .apply((RuntimeValue<AwsSyncClientBuilder<?, ?>>) builderInstance.getBuilder(),
                                syncSdkHttpClientBuilder.get());

                RuntimeValue<AwsClientBuilder> configuredSyncClientBuilder = recorder.configure(syncClientBuilder,
                        amazonClientConfigRuntime,
                        sdkBuildConfig, sharedExecutorService, configName, clientName);

                clientBuilders.produce(new AmazonClientBuilderBuildItem(configuredSyncClientBuilder, syncClientBuilderClass,
                        clientName, addOpenTelemetry));

                syntheticBeans
                        .produce(
                                namedClient(SyntheticBeanBuildItem.configure(syncClientName), clientName)
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
                AmazonClientExtensionBuilderInstanceBuildItem builderInstance = builderInstances.stream()
                        .filter(o -> o.getClientBuilderClass().equals(asyncClientBuilderClass) &&
                                o.getClientName().equals(clientName))
                        .findFirst().orElseThrow();

                RuntimeValue<AwsClientBuilder> asyncClientBuilder = asyncClientBuilderFunction
                        .apply((RuntimeValue<AwsAsyncClientBuilder<?, ?>>) builderInstance.getBuilder(),
                                asyncSdkAsyncHttpClientBuilder.get());

                RuntimeValue<AwsClientBuilder> configuredAsyncClientBuilder = recorder.configure(asyncClientBuilder,
                        amazonClientConfigRuntime,
                        sdkBuildConfig, sharedExecutorService, configName, clientName);

                clientBuilders.produce(new AmazonClientBuilderBuildItem(configuredAsyncClientBuilder, asyncClientBuilderClass,
                        clientName, addOpenTelemetry));

                syntheticBeans.produce(
                        namedClient(SyntheticBeanBuildItem.configure(asyncClientName), clientName)
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
                AmazonClientExtensionBuilderInstanceBuildItem builderInstance = builderInstances.stream()
                        .filter(o -> o.getClientBuilderClass().equals(presignerBuilderClass) &&
                                o.getClientName().equals(clientName))
                        .findFirst().orElseThrow();

                RuntimeValue<SdkPresigner.Builder> presignerBuilder = presignerBuilderSupplier
                        .apply((RuntimeValue<SdkPresigner.Builder>) builderInstance.getBuilder());

                presignerBuilder = recorder.configurePresigner(presignerBuilder, amazonClientConfigRuntime,
                        configName, clientName);
                syntheticBeans.produce(
                        namedBuilder(SyntheticBeanBuildItem.configure(presignerBuilderClass), clientName)
                                .unremovable()
                                .defaultBean()
                                .setRuntimeInit()
                                .scope(ApplicationScoped.class)
                                .runtimeValue(presignerBuilder)
                                .done());
                syntheticBeans.produce(
                        namedClient(SyntheticBeanBuildItem.configure(presignerClientName), clientName)
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

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void produceSyntheticBeans(
            AmazonClientOpenTelemetryRecorder otelRecorder,
            List<AmazonClientBuilderBuildItem> clientBuilders,
            List<AmazonClientBuilderOverrideBuildItem> clientBuilderOverride,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {

        for (AmazonClientBuilderBuildItem clientBuilder : clientBuilders) {
            Optional<AmazonClientBuilderOverrideBuildItem> override = clientBuilderOverride.stream()
                    .filter(o -> o.getBuilderClass().equals(clientBuilder.getBuilderClass()) &&
                            o.getClientName().equals(clientBuilder.getClientName()))
                    .findFirst();

            if (override.isEmpty()) {
                if (clientBuilder.hasOpenTelemetry()) {
                    syntheticBeans.produce(namedBuilder(SyntheticBeanBuildItem.configure(clientBuilder.getBuilderClass()),
                            clientBuilder.getClientName())
                            .unremovable()
                            .defaultBean()
                            .setRuntimeInit()
                            .scope(ApplicationScoped.class)
                            .createWith(otelRecorder.configure(clientBuilder.getClientBuilder()))
                            .addInjectionPoint(ClassType.create(AwsSdkTelemetry.class)).done());
                } else {
                    syntheticBeans.produce(namedBuilder(SyntheticBeanBuildItem.configure(clientBuilder.getBuilderClass()),
                            clientBuilder.getClientName())
                            .unremovable()
                            .defaultBean()
                            .setRuntimeInit()
                            .scope(ApplicationScoped.class)
                            .runtimeValue(clientBuilder.getClientBuilder())
                            .done());
                }
            }
        }
    }
}
