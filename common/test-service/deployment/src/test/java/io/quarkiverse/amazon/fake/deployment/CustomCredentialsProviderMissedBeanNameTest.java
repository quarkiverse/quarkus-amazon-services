package io.quarkiverse.amazon.fake.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.fake.FakeClient;

public class CustomCredentialsProviderMissedBeanNameTest {

    @Inject
    Instance<FakeClient> client;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .assertException(throwable -> assertEquals(
                    "quarkus.fake-service.aws.credentials.custom-provider.name cannot be empty if CUSTOM credentials provider used.",
                    throwable.getMessage()))
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.fake-service.aws.region", "us-east-2")
            .overrideConfigKey("quarkus.fake-service.aws.credentials.type", "custom");

    @Test
    void test() {
        Assertions.fail();
    }
}
