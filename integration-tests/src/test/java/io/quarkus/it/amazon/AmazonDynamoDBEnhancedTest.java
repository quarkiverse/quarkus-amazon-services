package io.quarkus.it.amazon;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Values;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AmazonDynamoDBEnhancedTest {

    @CartesianTest
    public void testDynamoDbEnhancedClientWithCustomExtension(
            @Values(strings = { "dynamodbenhanced", "dynamodbenhanceddbtable" }) String testedRresource,
            @Values(strings = { "async", "blocking" }) String path) {

        given()
                .pathParam("resource", testedRresource)
                .pathParam("path", path)
                .when().get("/test/{resource}/{path}")
                .then().body(is("INTERCEPTED EXTENSION OK@1"));
    }
}
