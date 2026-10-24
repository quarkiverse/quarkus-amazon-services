package io.quarkiverse.amazon.fake.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.fake.FakeAsyncClient;

/**
 * The default async client, netty, is required when no other async client is configured.
 */
public class MissingNettyClientTest {

    @Inject
    FakeAsyncClient client;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setForcedDependencies(SdkHttpClients.of())
            .withApplicationRoot((jar) -> jar.addClass(SdkHttpClients.class))
            .assertException(throwable -> assertEquals(
                    "Missing 'software.amazon.awssdk:netty-nio-client' dependency on the classpath",
                    throwable.getMessage()))
            .overrideConfigKey("quarkus.fake-service.devservices.enabled", "false");

    @Test
    public void test() {
        // should not be called, deployment exception should happen first.
        Assertions.fail();
    }
}
