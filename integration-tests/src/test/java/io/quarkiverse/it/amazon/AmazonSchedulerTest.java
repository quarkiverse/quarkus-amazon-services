package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonSchedulerTest {

    @Test
    public void testEventBridgeAsync() {
        RestAssured.when().get("/test/scheduler/async").then()
                .body(is("arn:aws:scheduler:us-east-1:000000000000:schedule/default/quarkus-schedule-async"));
    }

    @Test
    public void testEventBridgeSync() {
        RestAssured.when().get("/test/scheduler/sync").then()
                .body(is("arn:aws:scheduler:us-east-1:000000000000:schedule/default/quarkus-schedule-sync"));
    }
}
