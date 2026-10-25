package io.quarkiverse.amazon.fake.deployment;

import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.APACHE;
import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.NETTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Level;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.fake.FakeClient;

public class ApacheDeprecationWarningTest {

    @Inject
    FakeClient sync;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setForcedDependencies(SdkHttpClients.of(APACHE, NETTY))
            .withApplicationRoot((jar) -> jar.addClasses(SdkHttpClients.class))
            .overrideConfigKey("quarkus.fake-service.devservices.enabled", "false")
            .overrideConfigKey("quarkus.fake-service.aws.region", "us-east-1")
            .overrideConfigKey("quarkus.fake-service.sync-client.type", "apache")
            .setLogRecordPredicate(r -> r.getLevel().intValue() == Level.WARNING.intValue()
                    && r.getMessage().contains("deprecated the Apache HTTP client"))
            .assertLogRecords(records -> {
                assertEquals(1, records.size());
                assertTrue(records.get(0).getMessage().contains("fake-service"));
            });

    @Test
    public void test() {
        // the build log is checked by assertLogRecords
    }
}
