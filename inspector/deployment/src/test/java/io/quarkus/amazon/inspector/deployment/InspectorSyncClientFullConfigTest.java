package io.quarkus.amazon.inspector.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.inspector.InspectorAsyncClient;
import software.amazon.awssdk.services.inspector.InspectorClient;

public class InspectorSyncClientFullConfigTest {

    @Inject
    InspectorClient client;

    @Inject
    InspectorAsyncClient async;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("sync-urlconn-full-config.properties", "application.properties"));

    @Test
    public void test() {
        // should finish with success
    }
}
