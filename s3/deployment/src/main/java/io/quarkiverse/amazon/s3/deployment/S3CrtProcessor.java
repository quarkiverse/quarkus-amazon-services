package io.quarkiverse.amazon.s3.deployment;

import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.DeploymentException;

import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import io.quarkiverse.amazon.common.deployment.RequireAmazonClientInjectionBuildItem;
import io.quarkiverse.amazon.common.deployment.RequireAmazonClientTransportBuilderBuildItem;
import io.quarkiverse.amazon.common.runtime.ClientUtil;
import io.quarkiverse.amazon.common.runtime.SdkAutoCloseableDestroyer;
import io.quarkiverse.amazon.s3.runtime.S3BuildTimeConfig;
import io.quarkiverse.amazon.s3.runtime.S3Crt;
import io.quarkiverse.amazon.s3.runtime.S3CrtRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.runtime.RuntimeValue;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient;

public class S3CrtProcessor {

    public static final DotName S3CRT = DotName.createSimple(S3Crt.class);
    private static final DotName S3_ASYNC_CLIENT = DotName.createSimple(S3AsyncClient.class.getName());
    private static final DotName S3_CLIENT = DotName.createSimple(S3Client.class.getName());

    S3BuildTimeConfig buildTimeConfig;

    // used only as a key to differentiate aws client
    protected String configName() {
        return "s3.crt-client";
    }

    protected DotName asyncClientName() {
        return DotName.createSimple(S3CrtAsyncClient.class.getName());
    }

    @BuildStep(onlyIf = IsAmazonCrtS3ClientPresent.class)
    AdditionalBeanBuildItem qualifiers() {
        return new AdditionalBeanBuildItem(S3Crt.class);
    }

    @BuildStep(onlyIf = IsAmazonCrtS3ClientPresent.class)
    void discover(BeanRegistrationPhaseBuildItem beanRegistrationPhase,
            BuildProducer<RequireAmazonClientInjectionBuildItem> requireClientInjectionProducer) {
        Optional<DotName> asyncClassName = Optional.empty();

        // Discover all clients injections in order to determine if s3 crt async client
        // is required
        for (InjectionPointInfo injectionPoint : beanRegistrationPhase.getInjectionPoints()) {

            if (null == injectionPoint.getRequiredQualifier(S3CRT)) {
                continue;
            }

            Type injectedType = getInjectedType(injectionPoint);

            if (S3_ASYNC_CLIENT.equals(injectedType.name())) {
                asyncClassName = Optional.of(asyncClientName());
            }

            if (S3_CLIENT.equals(injectedType.name())) {
                throw new DeploymentException("@S3Crt is only valid with S3AsyncClient instance.");
            }
        }

        if (asyncClassName.isPresent()) {
            requireClientInjectionProducer
                    .produce(new RequireAmazonClientInjectionBuildItem(asyncClassName.get(), ClientUtil.DEFAULT_CLIENT_NAME));
        }
    }

    @BuildStep(onlyIf = IsAmazonCrtS3ClientPresent.class)
    void setupClient(List<RequireAmazonClientInjectionBuildItem> clientRequirements,
            BuildProducer<RequireAmazonClientTransportBuilderBuildItem> clientProducer) {
        Optional<DotName> asyncClassName = Optional.empty();

        for (RequireAmazonClientInjectionBuildItem clientRequirement : clientRequirements) {

            if (clientRequirement.getClassName().equals(asyncClientName())) {
                asyncClassName = Optional.of(clientRequirement.getClassName());
            }
        }

        if (asyncClassName.isPresent()) {
            clientProducer
                    .produce(new RequireAmazonClientTransportBuilderBuildItem(Optional.empty(), asyncClassName, configName(),
                            buildTimeConfig.sdk(), buildTimeConfig.syncClient(), buildTimeConfig.asyncClient()));
        }
    }

    @BuildStep(onlyIf = IsAmazonCrtS3ClientPresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void createS3CrtAsyncClient(List<RequireAmazonClientTransportBuilderBuildItem> amazonClients,
            S3CrtRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            ExecutorBuildItem executorBuildItem,
            LaunchModeBuildItem launchMode) {
        Optional<RequireAmazonClientTransportBuilderBuildItem> matchingClientBuildItem = amazonClients.stream()
                .filter(c -> c.getAwsClientName().equals(configName()))
                .findAny();

        matchingClientBuildItem.ifPresent(client -> {
            if (!client.getAsyncClassName().isPresent()) {
                return;
            }

            RuntimeValue<S3CrtAsyncClientBuilder> asyncClientBuilder = recorder.getCrtAsyncClientBuilder(configName());

            asyncClientBuilder = recorder.setExecutor(asyncClientBuilder, launchMode.getLaunchMode(),
                    executorBuildItem.getExecutorProxy());

            syntheticBeans.produce(SyntheticBeanBuildItem.configure(S3CrtAsyncClientBuilder.class)
                    .unremovable()
                    .setRuntimeInit()
                    .defaultBean()
                    .scope(ApplicationScoped.class)
                    .runtimeValue(asyncClientBuilder)
                    .done());

            syntheticBeans.produce(SyntheticBeanBuildItem.configure(S3AsyncClient.class)
                    .unremovable()
                    .setRuntimeInit()
                    .scope(ApplicationScoped.class)
                    .addQualifier(S3Crt.class)
                    .createWith(recorder.getS3CrtAsyncClient())
                    .destroyer(SdkAutoCloseableDestroyer.class)
                    .addInjectionPoint(ClassType.create(S3CrtAsyncClientBuilder.class))
                    .done());
        });
    }

    private Type getInjectedType(InjectionPointInfo injectionPoint) {
        Type requiredType = injectionPoint.getRequiredType();
        Type injectedType = requiredType;

        if (DotNames.INSTANCE.equals(requiredType.name()) && requiredType instanceof ParameterizedType) {
            injectedType = requiredType.asParameterizedType().arguments().get(0);
        }

        return injectedType;
    }

    public static final String AWS_S3_CRT = "software.amazon.awssdk.crt.s3.S3Client";

    public static class IsAmazonCrtS3ClientPresent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return QuarkusClassLoader.isClassPresentAtRuntime(AWS_S3_CRT);
        }
    }
}
