package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonIamTest {

    @Test
    public void testIamAsync() {
        RestAssured.when().get("/test/iam/async").then().body(any(String.class));
    }

    @Test
    public void testIamSync() {
        RestAssured.when().get("/test/iam/sync").then().body(any(String.class));
    }

    @Test
    public void testAccountSync() {
        RestAssured.when().get("/test/iam/account").then().body(equalTo("000000000000:112233445566"));
    }
}
