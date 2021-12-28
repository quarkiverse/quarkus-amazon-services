package io.quarkus.it.amazon;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonStsTest {

    @Test
    public void testStsAsync() {
        RestAssured.when()
                .get("/test/sts/async")
                .then()
                .body(is("Quarkus is awsome"));
    }

    @Test
    public void testStsSync() {
        RestAssured.when()
                .get("/test/sts/sync")
                .then()
                .body(is("Quarkus is awsome"));
    }
}
