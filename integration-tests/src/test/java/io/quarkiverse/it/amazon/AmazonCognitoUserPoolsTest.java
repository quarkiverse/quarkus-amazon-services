package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonCognitoUserPoolsTest {
    @Test
    public void testAsync() {
        RestAssured.when().get("/test/cognito-user-pools/async")
                .then().body(is("async-success"));
    }

    @Test
    public void testSync() {
        RestAssured.when().get("/test/cognito-user-pools/sync")
                .then().body(is("sync-success"));
    }
}
