package io.quarkiverse.amazon.fake.deployment;

import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.URL_CONNECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.fake.FakeClient;

public class CustomCredentialsProviderMissedBeanNameNamedClientTest {

    @Inject
    @AmazonClient("custom")
    Instance<FakeClient> client;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setForcedDependencies(SdkHttpClients.of(URL_CONNECTION))
            .withApplicationRoot((jar) -> jar.addClass(SdkHttpClients.class))
            .assertException(throwable -> assertEquals(
                    "quarkus.fake-service.\"custom\".aws.credentials.custom-provider.name cannot be empty if CUSTOM credentials provider used.",
                    throwable.getMessage()))
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.fake-service.aws.region", "us-east-2")
            .overrideConfigKey("quarkus.fake-service.custom.aws.credentials.type", "custom");

    @Test
    void test() {
        Assertions.fail();
    }
}
