package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.any;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonRedshiftTest {

    @Test
    public void testRedshift() {
        RestAssured.when().get("/test/redshift/test").then().body(any(String.class));
    }
}
