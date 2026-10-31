package io.quarkiverse.amazon.sdk.deployment;

import java.util.concurrent.CompletableFuture;

import jakarta.inject.Inject;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.test.AbstractAsyncClientVertxContextDisabledTest;
import io.quarkiverse.amazon.common.test.AsyncClientContextTestSupport;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.ecr.EcrAsyncClient;

public class SdkAsyncClientVertxContextDisabledTest extends AbstractAsyncClientVertxContextDisabledTest {
    @RegisterExtension
    static final QuarkusExtensionTest app = AsyncClientContextTestSupport
            .configure("quarkus.aws", "", AsyncClientContextResource.class)
            .overrideConfigKey("quarkus.aws.async-client.type", "netty")
            .overrideConfigKey("quarkus.aws.async-client.advanced.vertx-context-propagation", "false");

    @Inject
    EcrAsyncClient client;

    @Override
    protected CompletableFuture<?> call() {
        return client.describeRepositories();
    }
}
