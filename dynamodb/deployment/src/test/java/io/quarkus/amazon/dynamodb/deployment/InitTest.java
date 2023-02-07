package io.quarkus.amazon.dynamodb.deployment;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.hasItem;

import jakarta.inject.Inject;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class InitTest {

    @Inject
    DynamoDbClient client;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("localstack-init-script-config.properties", "application.properties"));

    @Test
    @DisplayName("QuarkusFruits table created by localstack init script")
    void test() {
        assertThat(client.listTables().tableNames(), hasItem("QuarkusFruits"));
    }
}