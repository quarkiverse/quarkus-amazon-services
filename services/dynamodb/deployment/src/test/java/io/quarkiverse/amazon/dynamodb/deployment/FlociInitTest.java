package io.quarkiverse.amazon.dynamodb.deployment;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.hasItem;

import jakarta.inject.Inject;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class FlociInitTest {

    @Inject
    DynamoDbClient client;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("floci-init-script-config.properties", "application.properties"));

    @Test
    @DisplayName("QuarkusFruits table created by floci init script")
    void test() {
        assertThat(client.listTables().tableNames(), hasItem("QuarkusFruits"));
    }
}