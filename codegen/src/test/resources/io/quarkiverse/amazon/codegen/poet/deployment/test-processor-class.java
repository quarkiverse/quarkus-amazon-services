package io.quarkiverse.amazon.ecr.deployment;

import io.quarkiverse.amazon.common.deployment.AbstractAmazonServiceProcessor;
import io.quarkiverse.amazon.common.deployment.AmazonClientExtensionBuildItem;
import io.quarkiverse.amazon.common.deployment.AmazonClientExtensionBuilderInstanceBuildItem;
import io.quarkiverse.amazon.common.deployment.RequireAmazonClientInjectionBuildItem;
import io.quarkiverse.amazon.common.runtime.HasSdkBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.HasTransportBuildTimeConfig;
import io.quarkiverse.amazon.ecr.runtime.EcrBuildTimeConfig;
import io.quarkiverse.amazon.ecr.runtime.EcrRecorder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import java.util.List;
import org.jboss.jandex.DotName;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.services.ecr.EcrAsyncClient;
import software.amazon.awssdk.services.ecr.EcrAsyncClientBuilder;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.EcrClientBuilder;

@Generated("io.quarkiverse.amazon:codegen")
public class EcrProcessor extends AbstractAmazonServiceProcessor {

    private static final String AMAZON_CLIENT_NAME = "amazon-sdk-ecr";

    EcrBuildTimeConfig buildTimeConfig;

    @Override
    protected String amazonServiceClientName() {
        return AMAZON_CLIENT_NAME;
    }

    @Override
    protected String configName() {
        return "ecr";
    }

    @Override
    protected DotName syncClientName() {
        return DotName.createSimple(EcrClient.class.getName());
    }

    @Override
    protected Class<?> syncClientBuilderClass() {
        return EcrClientBuilder.class;
    }

    @Override
    protected DotName asyncClientName() {
        return DotName.createSimple(EcrAsyncClient.class.getName());
    }

    @Override
    protected Class<?> asyncClientBuilderClass() {
        return EcrAsyncClientBuilder.class;
    }

    @Override
    protected String builtinInterceptorsPath() {
        return "software/amazon/awssdk/services/ecr/execution.interceptors";
    }

    @Override
    protected HasTransportBuildTimeConfig transportBuildTimeConfig() {
        return buildTimeConfig;
    }

    @Override
    protected HasSdkBuildTimeConfig sdkBuildTimeConfig() {
        return buildTimeConfig;
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void createBuilders(EcrRecorder recorder, List<RequireAmazonClientInjectionBuildItem> amazonClientInjections,
            BuildProducer<AmazonClientExtensionBuilderInstanceBuildItem> builderInstances) {
        createExtensionBuilders(recorder, amazonClientInjections, builderInstances);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setup(
            EcrRecorder recorder,
            BuildProducer<AmazonClientExtensionBuildItem> amazonExtensions) {

        setupExtension(recorder, amazonExtensions);
    }
}
