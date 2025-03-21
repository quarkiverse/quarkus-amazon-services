package io.quarkiverse.amazon.iot.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.iot.IotAsyncClient;
import software.amazon.awssdk.services.iot.IotClient;

public class IotClientFullConfigTest {

    @Inject
    IotClient client;

    @Inject
    IotAsyncClient async;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("sync-urlconn-full-config.properties", "application.properties"));

    @Test
    public void test() {
        // should finish with success
    }
}
