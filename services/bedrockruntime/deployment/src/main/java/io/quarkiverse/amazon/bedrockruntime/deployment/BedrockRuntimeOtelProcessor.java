package io.quarkiverse.amazon.bedrockruntime.deployment;

import static io.quarkiverse.amazon.common.deployment.ClientDeploymentUtil.namedBuilder;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.jandex.ClassType;

import io.opentelemetry.instrumentation.awssdk.v2_2.AwsSdkTelemetry;
import io.quarkiverse.amazon.bedrockruntime.runtime.BedrockRuntimeOpenTelemetryRecorder;
import io.quarkiverse.amazon.common.deployment.AmazonClientBuilderBuildItem;
import io.quarkiverse.amazon.common.deployment.AmazonClientBuilderOverrideBuildItem;
import io.quarkiverse.amazon.common.deployment.RequireAmazonTelemetryBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClientBuilder;

public class BedrockRuntimeOtelProcessor {
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void wrapClientBuilders(List<AmazonClientBuilderBuildItem> clientBuilders,
            List<RequireAmazonTelemetryBuildItem> amazonRequireTelemtryClients,
            BedrockRuntimeOpenTelemetryRecorder otelRecorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<AmazonClientBuilderOverrideBuildItem> overrides) {

        boolean addOpenTelemetry = amazonRequireTelemtryClients
                .stream()
                .anyMatch(c -> "bedrockruntime".equals(c.getConfigName()));

        if (!addOpenTelemetry) {
            return;
        }

        for (AmazonClientBuilderBuildItem clientBuilder : clientBuilders) {
            if (clientBuilder.getBuilderClass().equals(BedrockRuntimeAsyncClientBuilder.class)) {

                syntheticBeans.produce(namedBuilder(SyntheticBeanBuildItem.configure(clientBuilder.getBuilderClass()),
                        clientBuilder.getClientName())
                        .unremovable()
                        .defaultBean()
                        .setRuntimeInit()
                        .scope(ApplicationScoped.class)
                        .createWith(otelRecorder.configureAsync(clientBuilder.getClientBuilder()))
                        .addInjectionPoint(ClassType.create(AwsSdkTelemetry.class)).done());
                overrides.produce(new AmazonClientBuilderOverrideBuildItem(
                        clientBuilder.getBuilderClass(),
                        clientBuilder.getClientName()));
            }
        }
    }
}
