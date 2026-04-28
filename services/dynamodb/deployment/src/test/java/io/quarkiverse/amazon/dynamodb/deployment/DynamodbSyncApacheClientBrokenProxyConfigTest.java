package io.quarkiverse.amazon.dynamodb.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.runtime.RuntimeConfigurationError;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DynamodbSyncApacheClientBrokenProxyConfigTest {

    @Inject
    DynamoDbClient client;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setExpectedException(RuntimeConfigurationError.class)
            .withApplicationRoot((jar) -> jar
                    .addAsResource("sync-apache-broken-proxy-config.properties", "application.properties"));

    @Test
    public void test() {
        Assertions.fail();
    }
}
