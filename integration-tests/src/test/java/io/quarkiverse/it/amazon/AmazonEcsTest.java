package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonEcsTest {

    @Test
    public void testEcsAsync() {
        RestAssured.when().get("/test/ecs/async").then().body(containsString("quarkus-test-cluster"));
    }

    @Test
    public void testEcsSync() {
        RestAssured.when().get("/test/ecs/sync").then().body(containsString("quarkus-test-cluster"));
    }
}