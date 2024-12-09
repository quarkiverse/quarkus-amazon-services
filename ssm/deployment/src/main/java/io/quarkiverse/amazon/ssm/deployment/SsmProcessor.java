package io.quarkiverse.amazon.ssm.deployment;

import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkiverse.amazon.common.deployment.AbstractAmazonServiceProcessor;
import io.quarkiverse.amazon.common.deployment.AmazonClientExtensionBuildItem;
import io.quarkiverse.amazon.common.deployment.AmazonClientExtensionBuilderInstanceBuildItem;
import io.quarkiverse.amazon.common.deployment.RequireAmazonClientInjectionBuildItem;
import io.quarkiverse.amazon.common.runtime.HasSdkBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.HasTransportBuildTimeConfig;
import io.quarkiverse.amazon.ssm.runtime.SsmBuildTimeConfig;
import io.quarkiverse.amazon.ssm.runtime.SsmRecorder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import software.amazon.awssdk.services.ssm.SsmAsyncClient;
import software.amazon.awssdk.services.ssm.SsmAsyncClientBuilder;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;

public class SsmProcessor extends AbstractAmazonServiceProcessor {

    private static final String AMAZON_CLIENT_NAME = "amazon-sdk-ssm";

    SsmBuildTimeConfig buildTimeConfig;

    @Override
    protected String amazonServiceClientName() {
        return AMAZON_CLIENT_NAME;
    }

    @Override
    protected String configName() {
        return "ssm";
    }

    @Override
    protected DotName syncClientName() {
        return DotName.createSimple(SsmClient.class.getName());
    }

    @Override
    protected Class<?> syncClientBuilderClass() {
        return SsmClientBuilder.class;
    }

    @Override
    protected DotName asyncClientName() {
        return DotName.createSimple(SsmAsyncClient.class.getName());
    }

    @Override
    protected Class<?> asyncClientBuilderClass() {
        return SsmAsyncClientBuilder.class;
    }

    @Override
    protected String builtinInterceptorsPath() {
        return "software/amazon/awssdk/services/ssm/execution.interceptors";
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
    void createBuilders(SsmRecorder recorder, List<RequireAmazonClientInjectionBuildItem> amazonClientInjections,
            BuildProducer<AmazonClientExtensionBuilderInstanceBuildItem> builderIstances) {
        createExtensionBuilders(recorder, amazonClientInjections, builderIstances);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setup(
            SsmRecorder recorder,
            BuildProducer<AmazonClientExtensionBuildItem> amazonExtensions) {

        setupExtension(recorder, amazonExtensions);
    }
}
