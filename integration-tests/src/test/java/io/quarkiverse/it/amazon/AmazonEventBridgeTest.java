package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonEventBridgeTest {

    @Test
    public void testEventBridgeAsync() {
        RestAssured.when().get("/test/eventbridge/async").then()
                .body(is("arn:aws:events:us-east-1:000000000000:rule/quarkus-rule-async"));
    }

    @Test
    public void testEventBridgeSync() {
        RestAssured.when().get("/test/eventbridge/sync").then()
                .body(is("arn:aws:events:us-east-1:000000000000:rule/quarkus-rule-sync"));
    }
}
