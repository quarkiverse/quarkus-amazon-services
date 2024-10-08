package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonSesTest {

    @Test
    public void testSesAsync() {
        RestAssured.when().get("/test/ses/async").then().body(any(String.class), not(emptyString()));
    }

    @Test
    public void testSesSync() {
        RestAssured.when().get("/test/ses/sync").then().body(any(String.class), not(emptyString()));
    }
}
