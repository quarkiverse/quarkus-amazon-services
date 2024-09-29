package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

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
    public void testS3CrtAsync() {
        RestAssured.when().get("/test/s3/crt-async").then().body(anyOf(is("sample S3 object"),
                is("No bean found for required type [interface software.amazon.awssdk.services.s3.S3AsyncClient] and qualifiers [[@io.quarkiverse.amazon.s3.runtime.S3Crt()]]")));
    }

    @Test
    public void testS3Blocking() {
        RestAssured.when().get("/test/s3/blocking").then().body(is("INTERCEPTED+sample S3 object"));
    }

    @Test
    public void testCopyS3Async() {
        RestAssured.when().get("/test/s3-transfer-manager/async").then().body(is("sample S3 object"));
    }

    @Test
    public void testCopyS3CrtAsync() {
        RestAssured.when().get("/test/s3-transfer-manager/crt-async").then().body(anyOf(is("sample S3 object"),
                is("No bean found for required type [interface software.amazon.awssdk.services.s3.S3AsyncClient] and qualifiers [[@io.quarkiverse.amazon.s3.runtime.S3Crt()]]")));
    }
}
