package io.quarkus.it.amazon;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.specimpl.ResteasyUriInfo;
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
        String body = RestAssured.when().get("/test/s3/presign").then().body(startsWith("http://localhost:4566/sync-"))
                .extract().body().asString();
        UriInfo uriInfo = new ResteasyUriInfo(body, "");
        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
        assertTrue(params.containsKey("X-Amz-Algorithm"));
        assertTrue(params.containsKey("X-Amz-Date"));
        assertTrue(params.containsKey("X-Amz-SignedHeaders"));
        assertTrue(params.containsKey("X-Amz-Expires"));
        assertTrue(params.containsKey("X-Amz-Credential"));
        assertTrue(params.containsKey("X-Amz-Signature"));
    }

}
