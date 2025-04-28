package io.quarkiverse.amazon.sesv2.deployment;

import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkiverse.amazon.common.deployment.AbstractAmazonServiceProcessor;
import io.quarkiverse.amazon.common.deployment.AmazonClientExtensionBuildItem;
import io.quarkiverse.amazon.common.deployment.AmazonClientExtensionBuilderInstanceBuildItem;
import io.quarkiverse.amazon.common.deployment.RequireAmazonClientInjectionBuildItem;
import io.quarkiverse.amazon.common.runtime.HasSdkBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.HasTransportBuildTimeConfig;
import io.quarkiverse.amazon.sesv2.runtime.SesV2BuildTimeConfig;
import io.quarkiverse.amazon.sesv2.runtime.SesV2Recorder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import software.amazon.awssdk.services.sesv2.SesV2AsyncClient;
import software.amazon.awssdk.services.sesv2.SesV2AsyncClientBuilder;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.SesV2ClientBuilder;

public class SesV2Processor extends AbstractAmazonServiceProcessor {

    private static final String AMAZON_CLIENT_NAME = "amazon-sdk-sesv2";

    SesV2BuildTimeConfig buildTimeConfig;

    @Override
    protected String amazonServiceClientName() {
        return AMAZON_CLIENT_NAME;
    }

    @Override
    protected String configName() {
        return "sesv2";
    }

    @Override
    protected DotName syncClientName() {
        return DotName.createSimple(SesV2Client.class.getName());
    }

    @Override
    protected Class<?> syncClientBuilderClass() {
        return SesV2ClientBuilder.class;
    }

    @Override
    protected DotName asyncClientName() {
        return DotName.createSimple(SesV2AsyncClient.class.getName());
    }

    @Override
    protected Class<?> asyncClientBuilderClass() {
        return SesV2AsyncClientBuilder.class;
    }

    @Override
    protected String builtinInterceptorsPath() {
        return "software/amazon/awssdk/services/sesv2/execution.interceptors";
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
    void createBuilders(SesV2Recorder recorder, List<RequireAmazonClientInjectionBuildItem> amazonClientInjections,
            BuildProducer<AmazonClientExtensionBuilderInstanceBuildItem> builderIstances) {
        createExtensionBuilders(recorder, amazonClientInjections, builderIstances);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setup(
            SesV2Recorder recorder,
            BuildProducer<AmazonClientExtensionBuildItem> amazonExtensions) {

        setupExtension(recorder, amazonExtensions);
    }
}
