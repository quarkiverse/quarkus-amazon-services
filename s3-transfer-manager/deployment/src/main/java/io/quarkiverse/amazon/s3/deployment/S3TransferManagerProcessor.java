package io.quarkiverse.amazon.s3.deployment;

import static io.quarkiverse.amazon.common.deployment.ClientDeploymentUtil.getNamedClientInjection;
import static io.quarkiverse.amazon.common.deployment.ClientDeploymentUtil.injectionPointAnnotationsClient;
import static io.quarkiverse.amazon.common.deployment.ClientDeploymentUtil.namedClient;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkiverse.amazon.common.deployment.AmazonClientInterceptorsPathBuildItem;
import io.quarkiverse.amazon.common.deployment.RequireAmazonClientInjectionBuildItem;
import io.quarkiverse.amazon.common.runtime.ClientUtil;
import io.quarkiverse.amazon.common.runtime.SdkAutoCloseableDestroyer;
import io.quarkiverse.amazon.s3.deployment.S3CrtProcessor.IsAmazonCrtS3ClientPresent;
import io.quarkiverse.amazon.s3.runtime.S3Crt;
import io.quarkiverse.amazon.s3.runtime.S3TransferManagerProducer;
import io.quarkiverse.amazon.s3.runtime.S3TransferManagerRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

public class S3TransferManagerProcessor {

    public static final DotName S3CRT_CLIENT = DotName.createSimple(S3CrtAsyncClient.class);

    @BuildStep
    void setupExtension(
            BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<AmazonClientInterceptorsPathBuildItem> interceptors) {

        feature.produce(new FeatureBuildItem("amazon-sdk-s3-transfer-manager"));
    }

    @BuildStep
    AdditionalBeanBuildItem producer() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(S3TransferManagerProducer.class).setUnremovable().build();
    }

    @BuildStep
    void discoverManagers(BeanRegistrationPhaseBuildItem beans,
            BuildProducer<RequireAmazonClientInjectionBuildItem> clients,
            BuildProducer<S3TransferManagerBuildItem> managers) {
        if (new IsAmazonCrtS3ClientPresent().getAsBoolean()) {
            clients.produce(new RequireAmazonClientInjectionBuildItem(S3CRT_CLIENT, ClientUtil.DEFAULT_CLIENT_NAME));
            managers.produce(new S3TransferManagerBuildItem(ClientUtil.DEFAULT_CLIENT_NAME, true));
        }
        var discovered = new java.util.HashSet<String>();
        for (var injection : beans.getInjectionPoints()) {
            Type type = injection.getRequiredType();
            if (type.name().equals(DotNames.INSTANCE) && type.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                type = type.asParameterizedType().arguments().get(0);
            }
            if (!type.name().equals(DotName.createSimple(S3TransferManager.class))) {
                continue;
            }
            String name = getNamedClientInjection(injection);
            boolean crt = injection.getRequiredQualifier(S3CrtProcessor.S3CRT) != null;
            if (ClientUtil.isDefaultClient(name)) {
                continue;
            }
            if (discovered.add(crt + ":" + name)) {
                clients.produce(new RequireAmazonClientInjectionBuildItem(
                        crt ? S3CRT_CLIENT : DotName.createSimple(S3AsyncClient.class), name));
                managers.produce(new S3TransferManagerBuildItem(name, crt));
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void createTransferManagers(List<S3TransferManagerBuildItem> managers,
            S3TransferManagerRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        for (var manager : managers) {
            var qualifiers = new ArrayList<AnnotationInstance>(List.of(injectionPointAnnotationsClient(manager.clientName)));
            qualifiers.add(S3ClientRoutingProcessor.route(manager.clientName, manager.crt));
            if (manager.crt) {
                qualifiers.add(AnnotationInstance.builder(S3Crt.class).build());
            }
            var bean = namedClient(SyntheticBeanBuildItem.configure(S3TransferManager.class), manager.clientName)
                    .unremovable()
                    .setRuntimeInit()
                    .reserve(true)
                    .priority(manager.crt ? (ClientUtil.isDefaultClient(manager.clientName) ? -1 : -2) : 0)
                    .addQualifier(S3ClientRoutingProcessor.route(manager.clientName, manager.crt))
                    .scope(ApplicationScoped.class)
                    .createWith(recorder.getTransferManager(manager.clientName, manager.crt))
                    .destroyer(SdkAutoCloseableDestroyer.class)
                    .addInjectionPoint(ClassType.create(S3AsyncClient.class),
                            qualifiers.toArray(AnnotationInstance[]::new));
            if (manager.crt) {
                bean.addQualifier().annotation(S3Crt.class).done();
            }
            syntheticBeans.produce(bean.done());
        }
    }
}
