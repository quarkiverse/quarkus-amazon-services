package io.quarkiverse.amazon.dynamodb.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

public class DynamodbAsyncClientTlsKeyFileStoreConfigTest {

    @Inject
    DynamoDbAsyncClient client;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("async-tls-key-filestore-config.properties", "application.properties"));

    @Test
    public void test() {
        // Application should start with full config.
    }
}
