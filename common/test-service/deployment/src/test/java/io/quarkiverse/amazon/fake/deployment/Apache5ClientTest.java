package io.quarkiverse.amazon.fake.deployment;

import static io.quarkiverse.amazon.fake.deployment.FakeClientInspector.httpClientName;
import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.APACHE5;
import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.NETTY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.fake.FakeAsyncClient;
import software.amazon.awssdk.services.fake.FakeClient;

public class Apache5ClientTest {

    @Inject
    FakeClient sync;

    @Inject
    FakeAsyncClient async;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setForcedDependencies(SdkHttpClients.of(APACHE5, NETTY))
            .withApplicationRoot((jar) -> jar.addClasses(SdkHttpClients.class, FakeClientInspector.class))
            .overrideConfigKey("quarkus.fake-service.devservices.enabled", "false")
            .overrideConfigKey("quarkus.fake-service.aws.region", "us-east-1")
            .overrideConfigKey("quarkus.fake-service.sync-client.type", "apache5")
            .overrideConfigKey("quarkus.fake-service.sync-client.apache.max-connections", "10")
            .overrideConfigKey("quarkus.fake-service.sync-client.apache.proxy.enabled", "true")
            .overrideConfigKey("quarkus.fake-service.sync-client.apache.proxy.endpoint", "http://127.1.1.1");

    @Test
    public void test() {
        assertEquals("Apache5", httpClientName(sync));
        // netty is the default async client
        assertEquals("NettyNio", httpClientName(async));
    }
}
