package io.quarkus.amazon.common.runtime;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.TlsTrustManagersProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;

@Recorder
public class AmazonClientUrlConnectionTransportRecorder extends AbstractAmazonClientTransportRecorder {

    @SuppressWarnings("rawtypes")
    @Override
    public RuntimeValue<SdkHttpClient.Builder> configureSync(String clientName,
            RuntimeValue<SyncHttpClientConfig> syncConfigRuntime) {
        SyncHttpClientConfig syncConfig = syncConfigRuntime.getValue();
        validateTlsKeyManagersProvider(clientName, syncConfig.tlsKeyManagersProvider, "sync");
        validateTlsTrustManagersProvider(clientName, syncConfig.tlsTrustManagersProvider, "sync");
        UrlConnectionHttpClient.Builder builder = UrlConnectionHttpClient.builder();
        builder.connectionTimeout(syncConfig.connectionTimeout);
        builder.socketTimeout(syncConfig.socketTimeout);
        builder.tlsKeyManagersProvider(getTlsKeyManagersProvider(syncConfig.tlsKeyManagersProvider));
        TlsTrustManagersProvider tlsTrustManagerProvider = getTlsTrustManagersProvider(syncConfig.tlsTrustManagersProvider);
        if (tlsTrustManagerProvider != null) {
            builder.tlsTrustManagersProvider(tlsTrustManagerProvider);
        }
        return new RuntimeValue<>(builder);
    }
}
