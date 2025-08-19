package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonResourceGroupsTaggingApiTest {

    @Test
    public void testApiGatewayAsync() {
        RestAssured.when().get("/test/resourcegroupstaggingapi/async").then().body(is("Service not yet supported"));
    }

    @Test
    public void testApiGatewaySync() {
        RestAssured.when().get("/test/resourcegroupstaggingapi/sync").then().body(is("InternalServiceException"));
    }
}
