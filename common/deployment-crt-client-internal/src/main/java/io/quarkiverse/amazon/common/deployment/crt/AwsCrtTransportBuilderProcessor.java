package io.quarkiverse.amazon.common.deployment.crt;

import java.util.List;
import java.util.Optional;

import io.quarkiverse.amazon.common.deployment.AmazonClientAsyncTransportBuildItem;
import io.quarkiverse.amazon.common.deployment.AmazonClientExtensionBuildItem;
import io.quarkiverse.amazon.common.deployment.AmazonClientSyncTransportBuildItem;
import io.quarkiverse.amazon.common.deployment.AmazonHttpClientsCheckedBuildItem;
import io.quarkiverse.amazon.common.deployment.RequireAmazonClientTransportBuilderBuildItem;
import io.quarkiverse.amazon.common.runtime.AmazonClientAwsCrtTransportRecorder;
import io.quarkiverse.amazon.common.runtime.AsyncHttpClientBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.AsyncHttpClientConfig;
import io.quarkiverse.amazon.common.runtime.SyncHttpClientBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.runtime.RuntimeValue;

public class AwsCrtTransportBuilderProcessor {

    @BuildStep()
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(AmazonHttpClientsCheckedBuildItem.class)
    void setupAwsCrtSyncTransport(
            List<AmazonClientExtensionBuildItem> extensions,
            List<RequireAmazonClientTransportBuilderBuildItem> amazonClients,
            AmazonClientAwsCrtTransportRecorder transportRecorder,
            BuildProducer<AmazonClientSyncTransportBuildItem> syncTransports) {

        extensions.forEach(extension -> createAwsCrtSyncTransportBuilder(
                extension.getConfigName(),
                extension.getConfigPrefix(),
                amazonClients,
                transportRecorder,
                extension.getBuildSyncConfig(),
                extension.getSyncConfig(),
                syncTransports));
    }

    @BuildStep()
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(AmazonHttpClientsCheckedBuildItem.class)
    void setupAwsCrtAsyncTransport(
            List<AmazonClientExtensionBuildItem> extensions,
            List<RequireAmazonClientTransportBuilderBuildItem> amazonClients,
            AmazonClientAwsCrtTransportRecorder transportRecorder,
            BuildProducer<AmazonClientAsyncTransportBuildItem> asyncTransports) {

        extensions.forEach(extension -> createAwsCrtAsyncTransportBuilder(
                extension.getConfigName(),
                extension.getConfigPrefix(),
                amazonClients,
                transportRecorder,
                extension.getBuildAsyncConfig(),
                extension.getAsyncConfig(),
                asyncTransports));
    }

    void createAwsCrtSyncTransportBuilder(String configName, String configPrefix,
            List<RequireAmazonClientTransportBuilderBuildItem> amazonClients,
            AmazonClientAwsCrtTransportRecorder recorder,
            SyncHttpClientBuildTimeConfig buildSyncConfig,
            RuntimeValue<SyncHttpClientConfig> syncConfig,
            BuildProducer<AmazonClientSyncTransportBuildItem> clientSyncTransports) {

        Optional<RequireAmazonClientTransportBuilderBuildItem> matchingClientBuildItem = amazonClients.stream()
                .filter(c -> c.getAwsClientName().equals(configName))
                .findAny();

        matchingClientBuildItem.ifPresent(client -> {
            if (!client.getSyncClassName().isPresent()) {
                return;
            }
            if (buildSyncConfig.type() != SyncHttpClientBuildTimeConfig.SyncClientType.AWS_CRT) {
                return;
            }

            clientSyncTransports.produce(
                    new AmazonClientSyncTransportBuildItem(
                            client.getAwsClientName(),
                            client.getSyncClassName().get(),
                            recorder.configureSync(configPrefix, syncConfig)));
        });
    }

    void createAwsCrtAsyncTransportBuilder(
            String configName,
            String configPrefix,
            List<RequireAmazonClientTransportBuilderBuildItem> amazonClients,
            AmazonClientAwsCrtTransportRecorder recorder,
            AsyncHttpClientBuildTimeConfig buildAsyncConfig,
            RuntimeValue<AsyncHttpClientConfig> asyncConfig,
            BuildProducer<AmazonClientAsyncTransportBuildItem> clientAsyncTransports) {

        Optional<RequireAmazonClientTransportBuilderBuildItem> matchingClientBuildItem = amazonClients.stream()
                .filter(c -> c.getAwsClientName().equals(configName))
                .findAny();

        matchingClientBuildItem.ifPresent(client -> {
            if (!client.getAsyncClassName().isPresent()) {
                return;
            }
            if (buildAsyncConfig.type() != AsyncHttpClientBuildTimeConfig.AsyncClientType.AWS_CRT) {
                return;
            }

            clientAsyncTransports.produce(
                    new AmazonClientAsyncTransportBuildItem(
                            client.getAwsClientName(),
                            client.getAsyncClassName().get(),
                            recorder.configureAsync(configPrefix, asyncConfig)));
        });
    }
}
