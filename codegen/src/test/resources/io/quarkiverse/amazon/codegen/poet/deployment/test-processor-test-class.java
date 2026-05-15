package io.quarkiverse.amazon.ecr.deployment;

import io.quarkiverse.amazon.common.deployment.RequireAmazonClientInjectionBuildItem;
import io.quarkiverse.amazon.common.runtime.ClientUtil;
import io.quarkiverse.amazon.ecr.EcrTestRecorder;
import io.quarkiverse.amazon.ecr.runtime.EcrSyntheticBean;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.services.ecr.EcrAsyncClient;
import software.amazon.awssdk.services.ecr.EcrClient;

@Generated("io.quarkiverse.amazon:codegen")
public class EcrTestProcessor {
    public EcrTestProcessor() {
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    public void registerSyntheticBean(EcrTestRecorder recorder, BuildProducer<SyntheticBeanBuildItem> syntheticBeanProducer,
            BuildProducer<RequireAmazonClientInjectionBuildItem> requireAmazonClientInjectionProducer) {
        var asyncClientClassName = DotName.createSimple(EcrAsyncClient.class);
        var syncClientClassName = DotName.createSimple(EcrClient.class);
        var builder = SyntheticBeanBuildItem.configure(EcrSyntheticBean.class);
        builder.scope(jakarta.inject.Singleton.class);
        builder.unremovable();
        builder.addInjectionPoint(ParameterizedType.create(DotNames.INSTANCE, ClassType.create(asyncClientClassName)));
        builder.addInjectionPoint(ParameterizedType.create(DotNames.INSTANCE, ClassType.create(syncClientClassName)));
        builder.createWith(recorder.createSyntheticBean());
        syntheticBeanProducer.produce(builder.done());
        requireAmazonClientInjectionProducer.produce(new RequireAmazonClientInjectionBuildItem(asyncClientClassName,
                ClientUtil.DEFAULT_CLIENT_NAME));
        requireAmazonClientInjectionProducer.produce(new RequireAmazonClientInjectionBuildItem(syncClientClassName,
                ClientUtil.DEFAULT_CLIENT_NAME));
    }
}