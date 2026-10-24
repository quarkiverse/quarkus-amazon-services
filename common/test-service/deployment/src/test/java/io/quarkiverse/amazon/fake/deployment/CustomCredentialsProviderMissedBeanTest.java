package io.quarkiverse.amazon.fake.deployment;

import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.URL_CONNECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.fake.FakeClient;

public class CustomCredentialsProviderMissedBeanTest {

    @Inject
    Instance<FakeClient> client;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setForcedDependencies(SdkHttpClients.of(URL_CONNECTION))
            .withApplicationRoot((jar) -> jar.addClass(SdkHttpClients.class))
            .assertException(throwable -> assertEquals(
                    "cannot find bean 'missed-bean' specified in quarkus.fake-service.aws.credentials.custom-provider.name",
                    throwable.getMessage()))
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.fake-service.aws.region", "us-east-2")
            .overrideConfigKey("quarkus.fake-service.aws.credentials.type", "custom")
            .overrideConfigKey("quarkus.fake-service.aws.credentials.custom-provider.name", "missed-bean");

    @Test
    void test() {
        Assertions.fail();
    }
}
