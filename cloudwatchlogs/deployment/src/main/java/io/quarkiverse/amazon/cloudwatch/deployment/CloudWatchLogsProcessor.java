package io.quarkiverse.amazon.cloudwatch.deployment;

import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkiverse.amazon.cloudwatch.runtime.CloudWatchLogsBuildTimeConfig;
import io.quarkiverse.amazon.cloudwatch.runtime.CloudWatchLogsRecorder;
import io.quarkiverse.amazon.common.deployment.*;
import io.quarkiverse.amazon.common.runtime.*;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClientBuilder;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClientBuilder;

public class CloudWatchLogsProcessor extends AbstractAmazonServiceProcessor {

    private static final String AMAZON_CLIENT_NAME = "amazon-sdk-cloudwatchlogs";

    CloudWatchLogsBuildTimeConfig buildTimeConfig;

    @Override
    protected String amazonServiceClientName() {
        return AMAZON_CLIENT_NAME;
    }

    @Override
    protected String configName() {
        return "cloudwatchlogs";
    }

    @Override
    protected DotName syncClientName() {
        return DotName.createSimple(CloudWatchLogsClient.class.getName());
    }

    @Override
    protected Class<?> syncClientBuilderClass() {
        return CloudWatchLogsClientBuilder.class;
    }

    @Override
    protected DotName asyncClientName() {
        return DotName.createSimple(CloudWatchLogsAsyncClient.class.getName());
    }

    @Override
    protected Class<?> asyncClientBuilderClass() {
        return CloudWatchLogsAsyncClientBuilder.class;
    }

    @Override
    protected String builtinInterceptorsPath() {
        return "software/amazon/awssdk/services/cloudwatchlogs/execution.interceptors";
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
    void createBuilders(CloudWatchLogsRecorder recorder, List<RequireAmazonClientInjectionBuildItem> amazonClientInjections,
            BuildProducer<AmazonClientExtensionBuilderInstanceBuildItem> builderIstances) {
        createExtensionBuilders(recorder, amazonClientInjections, builderIstances);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setup(
            CloudWatchLogsRecorder recorder,
            BuildProducer<AmazonClientExtensionBuildItem> amazonExtensions) {

        setupExtension(recorder, amazonExtensions);
    }
}
