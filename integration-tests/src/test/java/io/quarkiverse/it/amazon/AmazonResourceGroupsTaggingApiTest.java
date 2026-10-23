package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonResourceGroupsTaggingApiTest {

    @Test
    public void testResourceGroupsTaggingApiAsync() {
        RestAssured.when().get("/test/resourcegroupstaggingapi/async").then()
                .statusCode(200)
                .body(allOf(startsWith("arn:aws:sqs:"), endsWith(":quarkus-rgta-async-queue")));
    }

    @Test
    public void testResourceGroupsTaggingApiSync() {
        RestAssured.when().get("/test/resourcegroupstaggingapi/sync").then()
                .statusCode(200)
                .body(allOf(startsWith("arn:aws:sqs:"), endsWith(":quarkus-rgta-sync-queue")));
    }
}
