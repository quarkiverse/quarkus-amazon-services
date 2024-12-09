package io.quarkiverse.amazon.common.deployment.netty;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import io.netty.channel.EventLoopGroup;
import io.quarkiverse.amazon.common.deployment.AmazonClientAsyncTransportBuildItem;
import io.quarkiverse.amazon.common.deployment.AmazonClientExtensionBuildItem;
import io.quarkiverse.amazon.common.deployment.RequireAmazonClientTransportBuilderBuildItem;
import io.quarkiverse.amazon.common.runtime.AmazonClientNettyTransportRecorder;
import io.quarkiverse.amazon.common.runtime.AsyncHttpClientBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.AsyncHttpClientConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.runtime.RuntimeValue;

public class NettyTransportBuilderProcessor {

    @BuildStep()
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupNettyAsyncTransport(
            List<AmazonClientExtensionBuildItem> extensions,
            List<RequireAmazonClientTransportBuilderBuildItem> amazonClients,
            AmazonClientNettyTransportRecorder transportRecorder,
            BuildProducer<AmazonClientAsyncTransportBuildItem> asyncTransports,
            EventLoopGroupBuildItem eventLoopSupplier) {

        extensions.forEach(extension -> createNettyAsyncTransportBuilder(
                extension.getConfigName(),
                amazonClients,
                transportRecorder,
                extension.getBuildAsyncConfig(),
                extension.getAsyncConfig(),
                asyncTransports,
                eventLoopSupplier.getMainEventLoopGroup()));
    }

    void createNettyAsyncTransportBuilder(String configName, List<RequireAmazonClientTransportBuilderBuildItem> amazonClients,
            AmazonClientNettyTransportRecorder recorder,
            AsyncHttpClientBuildTimeConfig buildAsyncConfig,
            RuntimeValue<AsyncHttpClientConfig> asyncConfig,
            BuildProducer<AmazonClientAsyncTransportBuildItem> clientAsyncTransports,
            Supplier<EventLoopGroup> eventLoopSupplier) {

        Optional<RequireAmazonClientTransportBuilderBuildItem> matchingClientBuildItem = amazonClients.stream()
                .filter(c -> c.getAwsClientName().equals(configName))
                .findAny();

        matchingClientBuildItem.ifPresent(client -> {
            if (!client.getAsyncClassName().isPresent()) {
                return;
            }
            if (buildAsyncConfig.type() != AsyncHttpClientBuildTimeConfig.AsyncClientType.NETTY) {
                return;
            }

            clientAsyncTransports.produce(
                    new AmazonClientAsyncTransportBuildItem(
                            client.getAwsClientName(),
                            client.getAsyncClassName().get(),
                            recorder.configureNettyAsync(recorder.configureAsync(configName, asyncConfig), eventLoopSupplier,
                                    asyncConfig)));
        });
    }

}
