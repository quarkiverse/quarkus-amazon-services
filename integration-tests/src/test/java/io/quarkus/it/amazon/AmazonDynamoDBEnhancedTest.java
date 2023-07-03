package io.quarkus.it.amazon;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junitpioneer.jupiter.CartesianProductTest;
import org.junitpioneer.jupiter.CartesianValueSource;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AmazonDynamoDBEnhancedTest {

    @CartesianProductTest
    @CartesianValueSource(strings = { "dynamodbenhanced", "dynamodbenhanceddbtable" })
    @CartesianValueSource(strings = { "async", "blocking" })
    public void testDynamoDbEnhancedClientWithCustomExtension(String testedRresource, String path) {

        given()
                .pathParam("resource", testedRresource)
                .pathParam("path", path)
                .when().get("/test/{resource}/{path}")
                .then().body(is("INTERCEPTED EXTENSION OK@1"));
    }
}
