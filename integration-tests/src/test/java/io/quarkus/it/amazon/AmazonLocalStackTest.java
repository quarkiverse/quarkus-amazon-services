package io.quarkus.it.amazon;

import static org.hamcrest.Matchers.any;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonLocalStackTest {

    @Test
    public void testSesAsync() {
        RestAssured.when().get("/test/redshift/test").then().body(any(String.class));
    }
}
