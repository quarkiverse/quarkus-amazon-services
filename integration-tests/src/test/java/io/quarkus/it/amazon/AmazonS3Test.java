package io.quarkus.it.amazon;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonS3Test {

    @Test
    public void testS3Async() {
        RestAssured.when().get("/test/s3/async").then().body(is("INTERCEPTED+sample S3 object"));
    }

    @Test
    public void testS3Blocking() {
        RestAssured.when().get("/test/s3/blocking").then().body(is("INTERCEPTED+sample S3 object"));
    }

    @Test
    public void testS3Presign() {
        RestAssured.when().get("/test/s3/presign").then()
                .body(startsWith("http://localhost:4566/sync-"))
                .body(containsString("X-Amz-Algorithm"))
                .body(containsString("X-Amz-Date"))
                .body(containsString("X-Amz-SignedHeaders"))
                .body(containsString("X-Amz-Expires"))
                .body(containsString("X-Amz-Credential"))
                .body(containsString("X-Amz-Signature"));
    }

}
