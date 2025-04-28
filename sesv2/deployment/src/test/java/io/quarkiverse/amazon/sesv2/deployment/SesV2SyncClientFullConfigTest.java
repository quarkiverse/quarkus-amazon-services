package io.quarkiverse.amazon.sesv2.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.sesv2.SesV2AsyncClient;
import software.amazon.awssdk.services.sesv2.SesV2Client;

public class SesV2SyncClientFullConfigTest {

    @Inject
    SesV2Client client;

    @Inject
    SesV2AsyncClient async;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("sync-urlconn-full-config.properties", "application.properties"));

    @Test
    public void test() {
        // should finish with success
    }
}
