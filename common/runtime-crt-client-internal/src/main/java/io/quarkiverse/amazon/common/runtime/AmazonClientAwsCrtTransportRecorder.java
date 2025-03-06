package io.quarkiverse.amazon.common.runtime;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;

@Recorder
public class AmazonClientAwsCrtTransportRecorder extends AbstractAmazonClientTransportRecorder {

    @SuppressWarnings("rawtypes")
    @Override
    public RuntimeValue<SdkAsyncHttpClient.Builder> configureAsync(String clientName,
            RuntimeValue<AsyncHttpClientConfig> asyncConfigRuntime) {

        AwsCrtAsyncHttpClient.Builder builder = AwsCrtAsyncHttpClient.builder();
        AsyncHttpClientConfig asyncConfig = asyncConfigRuntime.getValue();
        validateAwsCrtClientConfig(clientName, asyncConfig);

        builder.connectionMaxIdleTime(asyncConfig.connectionMaxIdleTime());
        builder.connectionTimeout(asyncConfig.connectionTimeout());
        builder.maxConcurrency(asyncConfig.maxConcurrency());

        if (asyncConfig.proxy().enabled() && asyncConfig.proxy().endpoint().isPresent()) {
            software.amazon.awssdk.http.crt.ProxyConfiguration.Builder proxyBuilder = software.amazon.awssdk.http.crt.ProxyConfiguration
                    .builder().scheme(asyncConfig.proxy().endpoint().get().getScheme())
                    .host(asyncConfig.proxy().endpoint().get().getHost());

            if (asyncConfig.proxy().endpoint().get().getPort() != -1) {
                proxyBuilder.port(asyncConfig.proxy().endpoint().get().getPort());
            }
            builder.proxyConfiguration(proxyBuilder.build());
        }

        return new RuntimeValue<>(builder);
    }

    private void validateAwsCrtClientConfig(String extension, AsyncHttpClientConfig config) {
        if (config.maxConcurrency() <= 0) {
            throw new RuntimeConfigurationError(
                    String.format("quarkus.%s.async-client.max-concurrency may not be negative or zero.", extension));
        }

        if (config.proxy().enabled()) {
            config.proxy().endpoint().ifPresent(uri -> validateProxyEndpoint(extension, uri, "async"));
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public RuntimeValue<SdkHttpClient.Builder> configureSync(String clientName,
            RuntimeValue<SyncHttpClientConfig> syncConfigRuntime) {

        AwsCrtHttpClient.Builder builder = AwsCrtHttpClient.builder();
        SyncHttpClientConfig syncConfig = syncConfigRuntime.getValue();
        validateAwsCrtClientConfig(clientName, syncConfig);

        builder.connectionTimeout(syncConfig.connectionTimeout());
        syncConfig.crt().connectionMaxIdleTime().ifPresent(builder::connectionMaxIdleTime);
        syncConfig.crt().maxConcurrency().ifPresent(builder::maxConcurrency);

        if (syncConfig.crt().proxy().enabled() && syncConfig.crt().proxy().endpoint().isPresent()) {
            software.amazon.awssdk.http.crt.ProxyConfiguration.Builder proxyBuilder = software.amazon.awssdk.http.crt.ProxyConfiguration
                    .builder().scheme(syncConfig.crt().proxy().endpoint().get().getScheme())
                    .host(syncConfig.crt().proxy().endpoint().get().getHost());
            if (syncConfig.crt().proxy().endpoint().get().getPort() != -1) {
                proxyBuilder.port(syncConfig.crt().proxy().endpoint().get().getPort());
            }
            syncConfig.crt().proxy().username().ifPresent(proxyBuilder::username);
            syncConfig.crt().proxy().password().ifPresent(proxyBuilder::password);

            builder.proxyConfiguration(proxyBuilder.build());
        }

        return new RuntimeValue<>(builder);
    }

    private void validateAwsCrtClientConfig(String extension, SyncHttpClientConfig config) {
        config.crt().maxConcurrency().ifPresent(maxConcurrency -> {
            if (maxConcurrency <= 0) {
                throw new RuntimeConfigurationError(
                        String.format("quarkus.%s.sync-client.crt.max-concurrency may not be negative or zero.", extension));
            }
        });

        if (config.crt().proxy().enabled()) {
            config.crt().proxy().endpoint().ifPresent(uri -> validateProxyEndpoint(extension, uri, "sync"));
        }
    }
}
