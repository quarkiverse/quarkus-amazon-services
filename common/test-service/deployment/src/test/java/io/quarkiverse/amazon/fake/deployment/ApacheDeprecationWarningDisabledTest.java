package io.quarkiverse.amazon.fake.deployment;

import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.APACHE;
import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.NETTY;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.fake.FakeClient;

public class ApacheDeprecationWarningDisabledTest {

    @Inject
    FakeClient sync;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setForcedDependencies(SdkHttpClients.of(APACHE, NETTY))
            .withApplicationRoot((jar) -> jar.addClasses(SdkHttpClients.class))
            .overrideConfigKey("quarkus.fake-service.devservices.enabled", "false")
            .overrideConfigKey("quarkus.fake-service.aws.region", "us-east-1")
            .overrideConfigKey("quarkus.fake-service.sync-client.type", "apache")
            .overrideConfigKey("quarkus.aws.sync-client.apache-deprecation-warning", "false")
            .setLogRecordPredicate(r -> r.getMessage().contains("deprecated the Apache HTTP client"))
            .assertLogRecords(records -> assertTrue(records.isEmpty()));

    @Test
    public void test() {
        // the build log is checked by assertLogRecords
    }
}
