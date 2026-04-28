package io.quarkiverse.amazon.inspector.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.inspector.InspectorAsyncClient;
import software.amazon.awssdk.services.inspector.InspectorClient;

public class InspectorSyncClientFullConfigTest {

    @Inject
    InspectorClient client;

    @Inject
    InspectorAsyncClient async;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("sync-urlconn-full-config.properties", "application.properties"));

    @Test
    public void test() {
        // should finish with success
    }
}
