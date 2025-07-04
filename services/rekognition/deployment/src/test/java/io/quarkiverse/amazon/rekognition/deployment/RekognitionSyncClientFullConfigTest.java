package io.quarkiverse.amazon.rekognition.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.rekognition.RekognitionAsyncClient;
import software.amazon.awssdk.services.rekognition.RekognitionClient;

public class RekognitionSyncClientFullConfigTest {

    @Inject
    RekognitionClient client;

    @Inject
    RekognitionAsyncClient async;

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
