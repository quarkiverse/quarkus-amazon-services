package io.quarkiverse.amazon.fake.deployment;

import java.util.concurrent.CompletableFuture;

import jakarta.inject.Inject;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkiverse.amazon.common.AmazonClientBuilder;
import io.quarkiverse.amazon.common.test.AbstractAsyncClientVertxContextTest;
import io.quarkiverse.amazon.common.test.AsyncClientContextTestSupport;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.services.fake.FakeAsyncClient;
import software.amazon.awssdk.services.fake.FakeAsyncClientBuilder;

public abstract class AbstractFakeAsyncClientVertxContextTest extends AbstractAsyncClientVertxContextTest {
    static QuarkusExtensionTest configure(String transport) {
        return AsyncClientContextTestSupport.configure("quarkus.fake-service", ".aws", AsyncClientContextResource.class,
                SdkHttpClients.class)
                .setForcedDependencies(SdkHttpClients.of(SdkHttpClients.URL_CONNECTION,
                        transport.equals("netty") ? SdkHttpClients.NETTY : SdkHttpClients.AWS_CRT))
                .overrideConfigKey("quarkus.fake-service.async-client.type", transport);
    }

    @Inject
    FakeAsyncClient client;
    @Inject
    @AmazonClient("archive")
    FakeAsyncClient named;
    @Inject
    FakeAsyncClientBuilder builder;
    @Inject
    @AmazonClientBuilder("custom-executor")
    FakeAsyncClientBuilder custom;
    @Inject
    @AmazonClient("custom-executor")
    FakeAsyncClient customClient; // Discover the named client and its provided builder together.

    @Override
    protected FakeAsyncClientBuilder customBuilder() {
        return custom;
    }

    @Override
    protected AwsClient client() {
        return client;
    }

    @Override
    protected AwsClient namedClient() {
        return named;
    }

    @Override
    protected FakeAsyncClientBuilder configuredBuilder() {
        return builder;
    }

    @Override
    protected FakeAsyncClientBuilder newBuilder() {
        return FakeAsyncClient.builder();
    }

    @Override
    protected CompletableFuture<?> call(AwsClient client) {
        return ((FakeAsyncClient) client).listThings();
    }
}
