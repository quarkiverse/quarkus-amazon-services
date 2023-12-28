package io.quarkus.it.amazon;

import static org.hamcrest.Matchers.any;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonSfnTest {

    @Test
    public void testSfnSync() {
        RestAssured.when()
                .get("/test/sfn/sync")
                .then()
                .body(any(String.class));
    }
}
