package io.quarkiverse.amazon.fake.deployment;

import java.util.concurrent.CompletableFuture;

import jakarta.inject.Inject;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.test.AbstractAsyncClientVertxContextDisabledTest;
import io.quarkiverse.amazon.common.test.AsyncClientContextTestSupport;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.fake.FakeAsyncClient;

public class FakeAsyncClientVertxContextDisabledTest extends AbstractAsyncClientVertxContextDisabledTest {
    @RegisterExtension
    static final QuarkusExtensionTest app = AsyncClientContextTestSupport.configure("quarkus.fake-service", ".aws",
            AsyncClientContextResource.class, SdkHttpClients.class)
            .setForcedDependencies(SdkHttpClients.of(SdkHttpClients.URL_CONNECTION, SdkHttpClients.NETTY))
            .overrideConfigKey("quarkus.fake-service.async-client.type", "netty")
            .overrideConfigKey("quarkus.fake-service.async-client.advanced.vertx-context-propagation", "false");

    @Inject
    FakeAsyncClient client;

    @Override
    protected CompletableFuture<?> call() {
        return client.listThings();
    }
}
