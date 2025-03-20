package io.quarkiverse.amazon.scheduler.deployment;

import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkiverse.amazon.common.deployment.AbstractAmazonServiceProcessor;
import io.quarkiverse.amazon.common.deployment.AmazonClientExtensionBuildItem;
import io.quarkiverse.amazon.common.deployment.AmazonClientExtensionBuilderInstanceBuildItem;
import io.quarkiverse.amazon.common.deployment.RequireAmazonClientInjectionBuildItem;
import io.quarkiverse.amazon.common.runtime.HasSdkBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.HasTransportBuildTimeConfig;
import io.quarkiverse.amazon.scheduler.runtime.SchedulerBuildTimeConfig;
import io.quarkiverse.amazon.scheduler.runtime.SchedulerRecorder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import software.amazon.awssdk.services.scheduler.SchedulerAsyncClient;
import software.amazon.awssdk.services.scheduler.SchedulerAsyncClientBuilder;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.SchedulerClientBuilder;

public class SchedulerProcessor extends AbstractAmazonServiceProcessor {

    private static final String AMAZON_CLIENT_NAME = "amazon-sdk-scheduler";

    SchedulerBuildTimeConfig buildTimeConfig;

    @Override
    protected String amazonServiceClientName() {
        return AMAZON_CLIENT_NAME;
    }

    @Override
    protected String configName() {
        return "eventbridge-scheduler";
    }

    @Override
    protected DotName syncClientName() {
        return DotName.createSimple(SchedulerClient.class.getName());
    }

    @Override
    protected Class<?> syncClientBuilderClass() {
        return SchedulerClientBuilder.class;
    }

    @Override
    protected DotName asyncClientName() {
        return DotName.createSimple(SchedulerAsyncClient.class.getName());
    }

    @Override
    protected Class<?> asyncClientBuilderClass() {
        return SchedulerAsyncClientBuilder.class;
    }

    @Override
    protected String builtinInterceptorsPath() {
        return "software/amazon/awssdk/services/scheduler/execution.interceptors";
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
    void createBuilders(SchedulerRecorder recorder, List<RequireAmazonClientInjectionBuildItem> amazonClientInjections,
            BuildProducer<AmazonClientExtensionBuilderInstanceBuildItem> builderIstances) {
        createExtensionBuilders(recorder, amazonClientInjections, builderIstances);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setup(
            SchedulerRecorder recorder,
            BuildProducer<AmazonClientExtensionBuildItem> amazonExtensions) {

        setupExtension(recorder, amazonExtensions);
    }
}
