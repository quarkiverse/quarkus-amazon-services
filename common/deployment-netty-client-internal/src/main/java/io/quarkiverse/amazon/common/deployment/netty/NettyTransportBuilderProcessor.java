package io.quarkiverse.amazon.common.deployment.netty;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import io.netty.channel.EventLoopGroup;
import io.netty.util.Version;
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

        boolean netty42 = isNetty42();
        extensions.forEach(extension -> createNettyAsyncTransportBuilder(
                extension.getConfigName(),
                amazonClients,
                transportRecorder,
                extension.getBuildAsyncConfig(),
                extension.getAsyncConfig(),
                asyncTransports,
                eventLoopSupplier.getMainEventLoopGroup(),
                netty42));
    }

    /**
     * Checks whether Netty 4.2 is on the classpath.
     * This is mainly for Quarkus 4 (which is using Netty 4.2). With this version of Netty, the AWS SDK cannot reuse
     * the Quarkus event loop group (incompatible).
     *
     * @return {@code true} if Netty 4.2 is on the classpath
     */
    private boolean isNetty42() {
        Version version = Version.identify().get("netty-buffer");
        if (version == null) {
            return false;
        }
        return version.artifactVersion().startsWith("4.2");
    }

    void createNettyAsyncTransportBuilder(String configName, List<RequireAmazonClientTransportBuilderBuildItem> amazonClients,
            AmazonClientNettyTransportRecorder recorder,
            AsyncHttpClientBuildTimeConfig buildAsyncConfig,
            RuntimeValue<AsyncHttpClientConfig> asyncConfig,
            BuildProducer<AmazonClientAsyncTransportBuildItem> clientAsyncTransports,
            Supplier<EventLoopGroup> eventLoopSupplier,
            boolean isNetty42) {

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
                            recorder.configureNettyAsync(recorder.configureAsync(configName, asyncConfig),
                                    configName,
                                    eventLoopSupplier,
                                    asyncConfig,
                                    isNetty42)));
        });
    }

}
