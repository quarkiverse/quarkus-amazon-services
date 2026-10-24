package io.quarkiverse.amazon.fake.deployment;

import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.NETTY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.fake.FakeAsyncClient;

/**
 * The configured async client is required, even if another async client is available.
 */
public class MissingAwsCrtAsyncClientTest {

    @Inject
    FakeAsyncClient client;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setForcedDependencies(SdkHttpClients.of(NETTY))
            .withApplicationRoot((jar) -> jar.addClass(SdkHttpClients.class))
            .assertException(throwable -> assertEquals(
                    "Missing 'software.amazon.awssdk:aws-crt-client' dependency on the classpath",
                    throwable.getMessage()))
            .overrideConfigKey("quarkus.fake-service.async-client.type", "aws-crt")
            .overrideConfigKey("quarkus.fake-service.devservices.enabled", "false");

    @Test
    public void test() {
        // should not be called, deployment exception should happen first.
        Assertions.fail();
    }
}
