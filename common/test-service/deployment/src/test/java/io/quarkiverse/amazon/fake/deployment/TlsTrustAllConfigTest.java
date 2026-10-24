package io.quarkiverse.amazon.fake.deployment;

import static io.quarkiverse.amazon.fake.deployment.FakeClientInspector.httpClientOption;
import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.NETTY;
import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.URL_CONNECTION;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.runtime.NoneTlsTrustManagersProvider;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.services.fake.FakeAsyncClient;
import software.amazon.awssdk.services.fake.FakeClient;

public class TlsTrustAllConfigTest {

    @Inject
    FakeClient sync;

    @Inject
    FakeAsyncClient async;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setForcedDependencies(SdkHttpClients.of(URL_CONNECTION, NETTY))
            .withApplicationRoot((jar) -> jar.addClasses(SdkHttpClients.class, FakeClientInspector.class))
            .overrideConfigKey("quarkus.fake-service.devservices.enabled", "false")
            .overrideConfigKey("quarkus.fake-service.aws.region", "us-east-1")
            .overrideConfigKey("quarkus.fake-service.sync-client.tls-trust-managers-provider.type", "trust-all")
            .overrideConfigKey("quarkus.fake-service.async-client.tls-trust-managers-provider.type", "trust-all");

    @Test
    public void test() {
        assertInstanceOf(NoneTlsTrustManagersProvider.class,
                httpClientOption(sync, SdkHttpConfigurationOption.TLS_TRUST_MANAGERS_PROVIDER));
        assertInstanceOf(NoneTlsTrustManagersProvider.class,
                httpClientOption(async, SdkHttpConfigurationOption.TLS_TRUST_MANAGERS_PROVIDER));
    }
}
