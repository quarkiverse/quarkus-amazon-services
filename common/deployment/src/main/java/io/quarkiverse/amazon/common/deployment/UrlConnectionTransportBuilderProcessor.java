package io.quarkiverse.amazon.common.deployment;

import java.util.List;
import java.util.Optional;

import io.quarkiverse.amazon.common.runtime.AmazonClientUrlConnectionTransportRecorder;
import io.quarkiverse.amazon.common.runtime.SyncHttpClientBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.runtime.RuntimeValue;

public class UrlConnectionTransportBuilderProcessor {

    @BuildStep()
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupUrlConnectionSyncTransport(
            List<AmazonClientExtensionBuildItem> extensions,
            List<RequireAmazonClientTransportBuilderBuildItem> amazonClients,
            AmazonClientUrlConnectionTransportRecorder transportRecorder,
            BuildProducer<AmazonClientSyncTransportBuildItem> syncTransports) {

        extensions.forEach(extension -> createUrlConnectionSyncTransportBuilder(
                extension.getConfigName(),
                amazonClients,
                transportRecorder,
                extension.getBuildSyncConfig(),
                extension.getSyncConfig(),
                syncTransports));
    }

    void createUrlConnectionSyncTransportBuilder(String configName,
            List<RequireAmazonClientTransportBuilderBuildItem> amazonClients,
            AmazonClientUrlConnectionTransportRecorder recorder,
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
            if (buildSyncConfig.type() != SyncHttpClientBuildTimeConfig.SyncClientType.URL) {
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
