package io.quarkus.it.amazon;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.it.amazon.lambda.LambdaResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonLambdaTest {

    @Test
    public void testS3Async() {
        RestAssured.when().get("/test/lambda/async").then().body(is(LambdaResource.OK));
    }

    @Test
    public void testS3Blocking() {
        RestAssured.when().get("/test/lambda/blocking").then().body(is(LambdaResource.OK));
    }

}
