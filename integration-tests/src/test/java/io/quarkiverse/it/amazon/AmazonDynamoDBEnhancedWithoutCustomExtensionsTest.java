package io.quarkiverse.it.amazon;

import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

@QuarkusTest
@TestProfile(Profiles.DynamoDBEnhancedClientWithoutCustomExtensions.class)
public class AmazonDynamoDBEnhancedWithoutCustomExtensionsTest {

    @Test
    public void testDynamoDbEnhancedClientWithoutCustomExtensionAsync() {
        RestAssured.when().get("/test/dynamodbenhanced/async").then().body(is("INTERCEPTED OK@1"));
    }

    @Test
    public void testDynamoDbEnhancedClientWithoutCustomExtensionBlocking() {
        RestAssured.when().get("/test/dynamodbenhanced/blocking").then().body(is("INTERCEPTED OK@1"));
    }
}
