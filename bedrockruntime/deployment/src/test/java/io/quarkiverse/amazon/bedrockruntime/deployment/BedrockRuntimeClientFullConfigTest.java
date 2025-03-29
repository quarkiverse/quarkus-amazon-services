package io.quarkiverse.amazon.bedrockruntime.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

class BedrockRuntimeClientFullConfigTest {

    @Inject
    BedrockRuntimeClient client;

    @Inject
    BedrockRuntimeAsyncClient async;

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
