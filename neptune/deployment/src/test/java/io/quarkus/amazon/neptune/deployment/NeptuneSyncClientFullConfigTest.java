package io.quarkus.amazon.neptune.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.neptune.NeptuneAsyncClient;
import software.amazon.awssdk.services.neptune.NeptuneClient;

public class NeptuneSyncClientFullConfigTest {

    @Inject
    NeptuneClient client;

    @Inject
    NeptuneAsyncClient async;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("sync-urlconn-full-config.properties", "application.properties"));

    @Test
    public void test() {
        // should finish with success
    }
}
