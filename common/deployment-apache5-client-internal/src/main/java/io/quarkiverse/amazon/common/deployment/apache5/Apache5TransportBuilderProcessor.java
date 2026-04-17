package io.quarkiverse.amazon.common.deployment.apache5;

import java.util.List;
import java.util.Optional;

import io.quarkiverse.amazon.common.deployment.AmazonClientExtensionBuildItem;
import io.quarkiverse.amazon.common.deployment.AmazonClientSyncTransportBuildItem;
import io.quarkiverse.amazon.common.deployment.RequireAmazonClientTransportBuilderBuildItem;
import io.quarkiverse.amazon.common.runtime.AmazonClientApache5TransportRecorder;
import io.quarkiverse.amazon.common.runtime.SyncHttpClientBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.runtime.RuntimeValue;

public class Apache5TransportBuilderProcessor {

    @BuildStep()
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupApache5SyncTransport(
            List<AmazonClientExtensionBuildItem> extensions,
            List<RequireAmazonClientTransportBuilderBuildItem> amazonClients,
            AmazonClientApache5TransportRecorder transportRecorder,
            BuildProducer<AmazonClientSyncTransportBuildItem> syncTransports) {

        extensions.forEach(extension -> createApache5SyncTransportBuilder(
                extension.getConfigName(),
                amazonClients,
                transportRecorder,
                extension.getBuildSyncConfig(),
                extension.getSyncConfig(),
                syncTransports));
    }

    void createApache5SyncTransportBuilder(String configName,
            List<RequireAmazonClientTransportBuilderBuildItem> amazonClients,
            AmazonClientApache5TransportRecorder recorder,
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
            if (buildSyncConfig.type() != SyncHttpClientBuildTimeConfig.SyncClientType.APACHE5) {
                return;
            }

            clientSyncTransports.produce(
                    new AmazonClientSyncTransportBuildItem(
                            client.getAwsClientName(),
                            client.getSyncClassName().get(),
                            recorder.configureSync(configName, syncConfig)));
        });
    }
}