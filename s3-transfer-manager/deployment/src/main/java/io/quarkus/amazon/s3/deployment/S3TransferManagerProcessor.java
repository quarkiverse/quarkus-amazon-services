package io.quarkus.amazon.s3.deployment;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;

import io.quarkus.amazon.common.deployment.AmazonClientInterceptorsPathBuildItem;
import io.quarkus.amazon.common.deployment.RequireAmazonClientInjectionBuildItem;
import io.quarkus.amazon.common.runtime.ClientUtil;
import io.quarkus.amazon.common.runtime.SdkAutoCloseableDestroyer;
import io.quarkus.amazon.s3.deployment.S3CrtProcessor.IsAmazonCrtS3ClientPresent;
import io.quarkus.amazon.s3.runtime.S3Crt;
import io.quarkus.amazon.s3.runtime.S3CrtTransferManagerRecorder;
import io.quarkus.amazon.s3.runtime.S3TransferManagerProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
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

        feature.produce(new FeatureBuildItem("amazon-s3-transfer-manager"));
    }

    @BuildStep
    AdditionalBeanBuildItem producer() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(S3TransferManagerProducer.class).setUnremovable().build();
    }

    @BuildStep(onlyIf = IsAmazonCrtS3ClientPresent.class)
    void requireS3CrtClient(BuildProducer<RequireAmazonClientInjectionBuildItem> requireClientInjectionProducer) {
        requireClientInjectionProducer
                .produce(new RequireAmazonClientInjectionBuildItem(S3CRT_CLIENT, ClientUtil.DEFAULT_CLIENT_NAME));
    }

    @BuildStep(onlyIf = IsAmazonCrtS3ClientPresent.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void createS3CrtTransferManager(
            S3CrtTransferManagerRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(S3TransferManager.class)
                .unremovable()
                .setRuntimeInit()
                .defaultBean()
                .addQualifier().annotation(S3Crt.class).done()
                .scope(ApplicationScoped.class)
                .createWith(recorder.getS3CrtTransferManager())
                .destroyer(SdkAutoCloseableDestroyer.class)
                .addInjectionPoint(ClassType.create(S3AsyncClient.class),
                        AnnotationInstance.builder(S3Crt.class).build())
                .done());
    }
}
