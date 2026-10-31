package io.quarkiverse.amazon.sdk.deployment;

import java.util.concurrent.CompletableFuture;

import jakarta.inject.Inject;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkiverse.amazon.common.AmazonClientBuilder;
import io.quarkiverse.amazon.common.test.AbstractAsyncClientVertxContextTest;
import io.quarkiverse.amazon.common.test.AsyncClientContextTestSupport;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.services.ecr.EcrAsyncClient;
import software.amazon.awssdk.services.ecr.EcrAsyncClientBuilder;

public abstract class AbstractSdkAsyncClientVertxContextTest extends AbstractAsyncClientVertxContextTest {
    static QuarkusExtensionTest configure() {
        return AsyncClientContextTestSupport.configure("quarkus.aws", "", AsyncClientContextResource.class);
    }

    @Inject
    EcrAsyncClient client;
    @Inject
    @AmazonClient("archive")
    EcrAsyncClient named;
    @Inject
    EcrAsyncClientBuilder builder;
    @Inject
    @AmazonClientBuilder("custom-executor")
    EcrAsyncClientBuilder custom;
    @Inject
    @AmazonClient("custom-executor")
    EcrAsyncClient customClient; // Discover the named client and its provided builder together.

    @Override
    protected EcrAsyncClientBuilder customBuilder() {
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
    protected EcrAsyncClientBuilder configuredBuilder() {
        return builder;
    }

    @Override
    protected EcrAsyncClientBuilder newBuilder() {
        return EcrAsyncClient.builder();
    }

    @Override
    protected CompletableFuture<?> call(AwsClient client) {
        return ((EcrAsyncClient) client).describeRepositories();
    }
}
