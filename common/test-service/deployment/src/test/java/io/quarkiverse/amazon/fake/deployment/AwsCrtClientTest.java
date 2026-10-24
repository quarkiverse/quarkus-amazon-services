package io.quarkiverse.amazon.fake.deployment;

import static io.quarkiverse.amazon.fake.deployment.FakeClientInspector.httpClientName;
import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.AWS_CRT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.fake.FakeAsyncClient;
import software.amazon.awssdk.services.fake.FakeClient;

public class AwsCrtClientTest {

    @Inject
    FakeClient sync;

    @Inject
    FakeAsyncClient async;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setForcedDependencies(SdkHttpClients.of(AWS_CRT))
            .withApplicationRoot((jar) -> jar.addClasses(SdkHttpClients.class, FakeClientInspector.class))
            .overrideConfigKey("quarkus.fake-service.devservices.enabled", "false")
            .overrideConfigKey("quarkus.fake-service.aws.region", "us-east-1")
            .overrideConfigKey("quarkus.fake-service.sync-client.type", "aws-crt")
            .overrideConfigKey("quarkus.fake-service.async-client.type", "aws-crt");

    @Test
    public void test() {
        assertEquals("AwsCommonRuntime", httpClientName(sync));
        assertEquals("AwsCommonRuntime", httpClientName(async));
    }
}
