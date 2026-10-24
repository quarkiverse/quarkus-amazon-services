package io.quarkiverse.amazon.fake.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.fake.FakeClient;

/**
 * The default sync client, url-connection, is required when no other sync client is configured.
 */
public class MissingUrlConnectionClientTest {

    @Inject
    FakeClient client;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setForcedDependencies(SdkHttpClients.of())
            .withApplicationRoot((jar) -> jar.addClass(SdkHttpClients.class))
            .assertException(throwable -> assertEquals(
                    "Missing 'software.amazon.awssdk:url-connection-client' dependency on the classpath",
                    throwable.getMessage()))
            .overrideConfigKey("quarkus.fake-service.devservices.enabled", "false");

    @Test
    public void test() {
        // should not be called, deployment exception should happen first.
        Assertions.fail();
    }
}
