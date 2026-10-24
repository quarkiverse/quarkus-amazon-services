package io.quarkiverse.amazon.fake.deployment;

import static io.quarkiverse.amazon.fake.deployment.FakeClientInspector.httpClientOption;
import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.NETTY;
import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.URL_CONNECTION;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.runtime.FileStoreTlsTrustManagersProvider;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.http.FileStoreTlsKeyManagersProvider;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.services.fake.FakeAsyncClient;
import software.amazon.awssdk.services.fake.FakeClient;

public class TlsFileStoreConfigTest {

    @Inject
    FakeClient sync;

    @Inject
    FakeAsyncClient async;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setForcedDependencies(SdkHttpClients.of(URL_CONNECTION, NETTY))
            .withApplicationRoot((jar) -> jar
                    .addClasses(SdkHttpClients.class, FakeClientInspector.class)
                    .addAsResource("tls-filestore-config.properties", "application.properties"));

    @Test
    public void test() {
        assertInstanceOf(FileStoreTlsKeyManagersProvider.class,
                httpClientOption(sync, SdkHttpConfigurationOption.TLS_KEY_MANAGERS_PROVIDER));
        assertInstanceOf(FileStoreTlsTrustManagersProvider.class,
                httpClientOption(sync, SdkHttpConfigurationOption.TLS_TRUST_MANAGERS_PROVIDER));
        assertInstanceOf(FileStoreTlsKeyManagersProvider.class,
                httpClientOption(async, SdkHttpConfigurationOption.TLS_KEY_MANAGERS_PROVIDER));
        assertInstanceOf(FileStoreTlsTrustManagersProvider.class,
                httpClientOption(async, SdkHttpConfigurationOption.TLS_TRUST_MANAGERS_PROVIDER));
    }
}
