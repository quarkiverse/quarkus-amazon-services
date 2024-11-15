package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonAcmTest {

    @Test
    public void testAcmAsync() {
        RestAssured.when().get("/test/acm/async").then().body(is("quarkus.local"));
    }

    @Test
    public void testAcmSync() {
        RestAssured.when().get("/test/acm/sync").then().body(is("quarkus.local"));
    }
}
