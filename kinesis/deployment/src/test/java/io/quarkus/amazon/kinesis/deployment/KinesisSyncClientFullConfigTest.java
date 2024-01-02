package io.quarkus.amazon.kinesis.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisClient;

public class KinesisSyncClientFullConfigTest {

    @Inject
    KinesisClient client;

    @Inject
    KinesisAsyncClient async;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("full-config.properties", "application.properties"));

    @Test
    public void test() {
        Assertions.assertNotNull(client);
        Assertions.assertNotNull(async);
    }
}
