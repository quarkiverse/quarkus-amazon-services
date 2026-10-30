package io.quarkiverse.amazon.sdk.runtime;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.quarkiverse.amazon.common.runtime.AmazonClientConfig;
import io.quarkiverse.amazon.common.runtime.AsyncHttpClientConfig;
import io.quarkiverse.amazon.common.runtime.ClientUtil;
import io.quarkiverse.amazon.common.runtime.HasAmazonClientRuntimeConfig;
import io.quarkiverse.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;

@Recorder
public class AmazonSdkRecorder {
    private final RuntimeValue<AmazonSdkConfig> config;

    public AmazonSdkRecorder(RuntimeValue<AmazonSdkConfig> config) {
        this.config = config;
    }

    public RuntimeValue<HasAmazonClientRuntimeConfig> configuration() {
        AmazonSdkConfig global = config.getValue();
        // Resolve on lookup: an injected named client need not have any explicit configuration.
        Map<String, AmazonClientConfig> clients = new AbstractMap<>() {
            @Override
            public AmazonClientConfig get(Object name) {
                return new ResolvedClientConfig(global.clients().get(name), global);
            }

            @Override
            public Set<Entry<String, AmazonClientConfig>> entrySet() {
                Set<String> names = new HashSet<>(global.clients().keySet());
                names.add(ClientUtil.DEFAULT_CLIENT_NAME);
                return names.stream().map(name -> Map.entry(name, get(name))).collect(Collectors.toSet());
            }
        };
        return new RuntimeValue<>(new HasAmazonClientRuntimeConfig() {
            @Override
            public Map<String, AmazonClientConfig> clients() {
                return clients;
            }

            @Override
            public AmazonClientConfig defaultsFor(String clientName) {
                // Named clients must never inherit another client's emulator settings.
                return clients.get(clientName);
            }

            @Override
            public String configPrefix(String extension, String clientName) {
                return HasAmazonClientRuntimeConfig.super.configPrefix("aws", clientName);
            }
        });
    }

    public RuntimeValue<SyncHttpClientConfig> syncConfig() {
        return new RuntimeValue<>(config.getValue().syncClient());
    }

    public RuntimeValue<AsyncHttpClientConfig> asyncConfig() {
        return new RuntimeValue<>(config.getValue().asyncClient());
    }

    public RuntimeValue<AwsSyncClientBuilder<?, ?>> syncBuilder(RuntimeValue<Supplier<AwsSyncClientBuilder<?, ?>>> factory) {
        return new RuntimeValue<>(factory.getValue().get());
    }

    public RuntimeValue<AwsAsyncClientBuilder<?, ?>> asyncBuilder(RuntimeValue<Supplier<AwsAsyncClientBuilder<?, ?>>> factory) {
        return new RuntimeValue<>(factory.getValue().get());
    }
}
