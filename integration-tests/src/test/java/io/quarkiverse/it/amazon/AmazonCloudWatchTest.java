package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.is;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonCloudWatchTest {
    @Test
    public void testAsync() {
        UUID namespace = UUID.randomUUID();
        RestAssured.given().formParam("namespace", namespace.toString()).post("/test/cloudwatch/async")
                .then().statusCode(200).body(is("Sum of Invocations for " + namespace + " is 1.0"));
    }

    @Test
    public void testSync() {
        UUID namespace = UUID.randomUUID();
        RestAssured.given().formParam("namespace", namespace.toString()).post("/test/cloudwatch/sync")
                .then().statusCode(200).body(is("Sum of Invocations for " + namespace + " is 1.0"));
    }
}
