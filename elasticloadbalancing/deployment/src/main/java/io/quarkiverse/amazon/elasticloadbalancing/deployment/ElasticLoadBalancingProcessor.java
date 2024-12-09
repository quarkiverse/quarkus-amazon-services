package io.quarkiverse.amazon.elasticloadbalancing.deployment;

import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkiverse.amazon.common.deployment.AbstractAmazonServiceProcessor;
import io.quarkiverse.amazon.common.deployment.AmazonClientExtensionBuildItem;
import io.quarkiverse.amazon.common.deployment.AmazonClientExtensionBuilderInstanceBuildItem;
import io.quarkiverse.amazon.common.deployment.RequireAmazonClientInjectionBuildItem;
import io.quarkiverse.amazon.common.runtime.HasSdkBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.HasTransportBuildTimeConfig;
import io.quarkiverse.amazon.elasticloadbalancing.runtime.ElasticLoadBalancingBuildTimeConfig;
import io.quarkiverse.amazon.elasticloadbalancing.runtime.ElasticLoadBalancingRecorder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import software.amazon.awssdk.services.elasticloadbalancing.ElasticLoadBalancingAsyncClient;
import software.amazon.awssdk.services.elasticloadbalancing.ElasticLoadBalancingAsyncClientBuilder;
import software.amazon.awssdk.services.elasticloadbalancing.ElasticLoadBalancingClient;
import software.amazon.awssdk.services.elasticloadbalancing.ElasticLoadBalancingClientBuilder;

public class ElasticLoadBalancingProcessor extends AbstractAmazonServiceProcessor {

    private static final String AMAZON_CLIENT_NAME = "amazon-sdk-elasticloadbalancing";

    ElasticLoadBalancingBuildTimeConfig buildTimeConfig;

    @Override
    protected String amazonServiceClientName() {
        return AMAZON_CLIENT_NAME;
    }

    @Override
    protected String configName() {
        return "elasticloadbalancing";
    }

    @Override
    protected DotName syncClientName() {
        return DotName.createSimple(ElasticLoadBalancingClient.class.getName());
    }

    @Override
    protected Class<?> syncClientBuilderClass() {
        return ElasticLoadBalancingClientBuilder.class;
    }

    @Override
    protected DotName asyncClientName() {
        return DotName.createSimple(ElasticLoadBalancingAsyncClient.class.getName());
    }

    @Override
    protected Class<?> asyncClientBuilderClass() {
        return ElasticLoadBalancingAsyncClientBuilder.class;
    }

    @Override
    protected String builtinInterceptorsPath() {
        return "software/amazon/awssdk/services/elasticloadbalancing/execution.interceptors";
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
    void createBuilders(ElasticLoadBalancingRecorder recorder,
            List<RequireAmazonClientInjectionBuildItem> amazonClientInjections,
            BuildProducer<AmazonClientExtensionBuilderInstanceBuildItem> builderIstances) {
        createExtensionBuilders(recorder, amazonClientInjections, builderIstances);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setup(
            ElasticLoadBalancingRecorder recorder,
            BuildProducer<AmazonClientExtensionBuildItem> amazonExtensions) {

        setupExtension(recorder, amazonExtensions);
    }
}
