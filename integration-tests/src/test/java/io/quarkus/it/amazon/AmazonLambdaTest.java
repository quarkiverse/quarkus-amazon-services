package io.quarkus.it.amazon;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonLambdaTest {

    public static final String LAMBDA_NAME = "localstack-lambda-hello";

    @Test
    public void testAsync() {
        RestAssured.given().when().get("/test/lambda/async").then().body(is(LAMBDA_NAME));
    }

    @Test
    public void testBlocking() {
        RestAssured.given().when().get("/test/lambda/blocking").then().body(is(LAMBDA_NAME));
    }
}
