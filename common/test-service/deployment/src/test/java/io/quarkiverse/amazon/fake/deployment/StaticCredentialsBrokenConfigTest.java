package io.quarkiverse.amazon.fake.deployment;

import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.NETTY;
import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.URL_CONNECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.fake.FakeAsyncClient;
import software.amazon.awssdk.services.fake.FakeClient;

public class StaticCredentialsBrokenConfigTest {

    @Inject
    FakeClient sync;

    @Inject
    FakeAsyncClient async;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setForcedDependencies(SdkHttpClients.of(URL_CONNECTION, NETTY))
            .withApplicationRoot((jar) -> jar.addClass(SdkHttpClients.class))
            .assertException(error -> assertEquals("quarkus.fake-service.aws.credentials.static-provider.access-key-id and "
                    + "quarkus.fake-service.aws.credentials.static-provider.secret-access-key "
                    + "cannot be empty if STATIC credentials provider used.", error.getMessage()))
            .overrideConfigKey("quarkus.fake-service.devservices.enabled", "false")
            .overrideConfigKey("quarkus.fake-service.aws.credentials.type", "static");

    @Test
    public void test() {
        // should not be called, deployment exception should happen first.
        Assertions.fail();
    }
}
