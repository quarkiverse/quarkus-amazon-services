package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonApiGatewayTest {

    @Test
    public void testApiGatewayAsync() {
        RestAssured.when().get("/test/apigateway/async").then().body(is("Test API Async"));
    }

    @Test
    public void testApiGatewaySync() {
        RestAssured.when().get("/test/apigateway/sync").then().body(is("Test API"));
    }
}
