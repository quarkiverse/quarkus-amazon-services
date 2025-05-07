package io.quarkiverse.amazon.bedrock;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.bedrock.BedrockAsyncClient;
import software.amazon.awssdk.services.bedrock.BedrockClient;

class BedrockClientFullConfigTest {

    @Inject
    BedrockClient client;

    @Inject
    BedrockAsyncClient async;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("full-config.properties", "application.properties"));

    @Test
    void test() {
        Assertions.assertNotNull(client);
        Assertions.assertNotNull(async);
    }
}
