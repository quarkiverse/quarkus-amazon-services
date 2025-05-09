package io.quarkiverse.amazon.inspector2.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.inspector2.Inspector2AsyncClient;
import software.amazon.awssdk.services.inspector2.Inspector2Client;

public class Inspector2SyncClientFullConfigTest {

    @Inject
    Inspector2Client client;

    @Inject
    Inspector2AsyncClient async;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("sync-urlconn-full-config.properties", "application.properties"));

    @Test
    public void test() {
        // should finish with success
    }
}
