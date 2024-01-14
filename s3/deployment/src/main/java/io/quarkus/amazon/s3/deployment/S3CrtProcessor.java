package io.quarkus.amazon.s3.deployment;

import java.util.Optional;
import java.util.function.BooleanSupplier;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import io.quarkus.amazon.common.runtime.SdkAutoCloseableDestroyer;
import io.quarkus.amazon.s3.runtime.S3Crt;
import io.quarkus.amazon.s3.runtime.S3CrtRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.runtime.RuntimeValue;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;

public class S3CrtProcessor {

    public static final DotName S3CRT = DotName.createSimple(S3Crt.class);

    protected String configName() {
        return "s3";
    }

    protected DotName asyncClientName() {
        return DotName.createSimple(S3AsyncClient.class.getName());
    }

    @BuildStep(onlyIf = IsAmazonCrtS3ClientPresent.class)
    AdditionalBeanBuildItem qualifiers() {
        return AdditionalBeanBuildItem.unremovableOf(S3Crt.class);
    }

    @BuildStep(onlyIf = IsAmazonCrtS3ClientPresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void createS3CrtClientBuilders(S3CrtRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BeanRegistrationPhaseBuildItem beanRegistrationPhase) {

        Optional<DotName> asyncClassName = Optional.empty();

        // Discover all clients injections in order to determine if crt async client
        // is required
        for (InjectionPointInfo injectionPoint : beanRegistrationPhase.getInjectionPoints()) {

            if (null == injectionPoint.getRequiredQualifier(S3CRT)) {
                continue;
            }

            Type injectedType = getInjectedType(injectionPoint);

            if (asyncClientName().equals(injectedType.name())) {
                asyncClassName = Optional.of(asyncClientName());
            }
        }

        if (asyncClassName.isPresent()) {
            RuntimeValue<S3CrtAsyncClientBuilder> asyncClientBuilder = recorder.getCrtAsyncClientBuilder(configName());
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

    public static final String AWS_S3_CRT = "software.amazon.awssdk.crt.s3.S3Client";

    public static class IsAmazonCrtS3ClientPresent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName(AWS_S3_CRT);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
