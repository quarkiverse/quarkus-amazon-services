package io.quarkiverse.amazon.elasticloadbalancingv2.deployment;

import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkiverse.amazon.common.deployment.AbstractAmazonServiceProcessor;
import io.quarkiverse.amazon.common.deployment.AmazonClientExtensionBuildItem;
import io.quarkiverse.amazon.common.deployment.AmazonClientExtensionBuilderInstanceBuildItem;
import io.quarkiverse.amazon.common.deployment.RequireAmazonClientInjectionBuildItem;
import io.quarkiverse.amazon.common.runtime.HasSdkBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.HasTransportBuildTimeConfig;
import io.quarkiverse.amazon.elasticloadbalancingv2.runtime.ElasticLoadBalancingV2BuildTimeConfig;
import io.quarkiverse.amazon.elasticloadbalancingv2.runtime.ElasticLoadBalancingV2Recorder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2AsyncClient;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2AsyncClientBuilder;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2ClientBuilder;

public class ElasticLoadBalancingV2Processor extends AbstractAmazonServiceProcessor {

    private static final String AMAZON_CLIENT_NAME = "amazon-sdk-elasticloadbalancingv2";

    ElasticLoadBalancingV2BuildTimeConfig buildTimeConfig;

    @Override
    protected String amazonServiceClientName() {
        return AMAZON_CLIENT_NAME;
    }

    @Override
    protected String configName() {
        return "elasticloadbalancingv2";
    }

    @Override
    protected DotName syncClientName() {
        return DotName.createSimple(ElasticLoadBalancingV2Client.class.getName());
    }

    @Override
    protected Class<?> syncClientBuilderClass() {
        return ElasticLoadBalancingV2ClientBuilder.class;
    }

    @Override
    protected DotName asyncClientName() {
        return DotName.createSimple(ElasticLoadBalancingV2AsyncClient.class.getName());
    }

    @Override
    protected Class<?> asyncClientBuilderClass() {
        return ElasticLoadBalancingV2AsyncClientBuilder.class;
    }

    @Override
    protected String builtinInterceptorsPath() {
        return "software/amazon/awssdk/services/elasticloadbalancingv2/execution.interceptors";
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
    void createBuilders(ElasticLoadBalancingV2Recorder recorder,
            List<RequireAmazonClientInjectionBuildItem> amazonClientInjections,
            BuildProducer<AmazonClientExtensionBuilderInstanceBuildItem> builderIstances) {
        createExtensionBuilders(recorder, amazonClientInjections, builderIstances);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setup(
            ElasticLoadBalancingV2Recorder recorder,
            BuildProducer<AmazonClientExtensionBuildItem> amazonExtensions) {

        setupExtension(recorder, amazonExtensions);
    }
}
