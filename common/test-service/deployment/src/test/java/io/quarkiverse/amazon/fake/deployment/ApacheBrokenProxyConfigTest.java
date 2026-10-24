package io.quarkiverse.amazon.fake.deployment;

import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.APACHE;
import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.NETTY;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.runtime.RuntimeConfigurationError;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.fake.FakeAsyncClient;
import software.amazon.awssdk.services.fake.FakeClient;

public class ApacheBrokenProxyConfigTest {

    @Inject
    FakeClient sync;

    @Inject
    FakeAsyncClient async;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setForcedDependencies(SdkHttpClients.of(APACHE, NETTY))
            .withApplicationRoot((jar) -> jar.addClass(SdkHttpClients.class))
            .setExpectedException(RuntimeConfigurationError.class)
            .overrideConfigKey("quarkus.fake-service.devservices.enabled", "false")
            .overrideConfigKey("quarkus.fake-service.sync-client.type", "apache")
            .overrideConfigKey("quarkus.fake-service.sync-client.apache.proxy.enabled", "true")
            .overrideConfigKey("quarkus.fake-service.sync-client.apache.proxy.endpoint", "http://user:name@127.1.1.1?foo=bar");

    @Test
    public void test() {
        // should not be called, deployment exception should happen first.
        Assertions.fail();
    }
}
