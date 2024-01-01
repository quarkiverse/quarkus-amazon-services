package io.quarkus.it.amazon;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonSfnTest {

    @ParameterizedTest
    @ValueSource(strings = { "sync", "async" })
    public void testSfn(String endpoint) {
        final String expectedBody = String.format("arn:aws:states:us-east-1:000000000000:stateMachine:%s-state-machine",
                endpoint);

        RestAssured.given()
                .pathParam("endpoint", endpoint)
                .when().get("/test/sfn/{endpoint}")
                .then()
                .body(is(expectedBody));
    }

}
