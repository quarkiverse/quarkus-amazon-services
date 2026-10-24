package io.quarkiverse.amazon.fake.deployment;

import static io.quarkiverse.amazon.fake.deployment.FakeClientInspector.httpClientName;
import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.APACHE;
import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.AWS_CRT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.fake.FakeAsyncClient;
import software.amazon.awssdk.services.fake.FakeClient;

public class ApacheAndAwsCrtClientTest {

    @Inject
    FakeClient sync;

    @Inject
    FakeAsyncClient async;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setForcedDependencies(SdkHttpClients.of(APACHE, AWS_CRT))
            .withApplicationRoot((jar) -> jar
                    .addClasses(SdkHttpClients.class, FakeClientInspector.class)
                    .addAsResource("apache-awscrt-full-config.properties", "application.properties"));

    @Test
    public void test() {
        assertEquals("Apache", httpClientName(sync));
        assertEquals("AwsCommonRuntime", httpClientName(async));
    }
}
