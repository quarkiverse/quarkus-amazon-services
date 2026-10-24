package io.quarkiverse.amazon.fake.deployment;

import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.URL_CONNECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.fake.FakeClient;

/**
 * The configured sync client is required, even if another sync client is available.
 */
public class MissingApache5ClientTest {

    @Inject
    FakeClient client;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setForcedDependencies(SdkHttpClients.of(URL_CONNECTION))
            .withApplicationRoot((jar) -> jar.addClass(SdkHttpClients.class))
            .assertException(throwable -> assertEquals(
                    "Missing 'software.amazon.awssdk:apache5-client' dependency on the classpath",
                    throwable.getMessage()))
            .overrideConfigKey("quarkus.fake-service.sync-client.type", "apache5")
            .overrideConfigKey("quarkus.fake-service.devservices.enabled", "false");

    @Test
    public void test() {
        // should not be called, deployment exception should happen first.
        Assertions.fail();
    }
}
