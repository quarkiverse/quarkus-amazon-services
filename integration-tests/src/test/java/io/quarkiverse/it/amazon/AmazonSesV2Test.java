package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonSesV2Test {

    @Test
    public void testSesV2Async() {
        RestAssured.when().get("/test/sesv2/async").then().statusCode(200).body(not(emptyString()));
    }

    @Test
    public void testSesV2Sync() {
        RestAssured.when().get("/test/sesv2/sync").then().statusCode(200).body(not(emptyString()));
    }
}
