package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.RestAssured;

@QuarkusIntegrationTest
public class AmazonS3ITCase extends AmazonS3Test {
    DevServicesContext context;

    @Test
    public void testS3Presign() {
        RestAssured.when().get("/test/s3/presign").then()
                .body(startsWith(context.devServicesProperties().get("quarkus.s3.endpoint-override") + "/sync-"))
                .body(containsString("X-Amz-Algorithm"))
                .body(containsString("X-Amz-Date"))
                .body(containsString("X-Amz-SignedHeaders"))
                .body(containsString("X-Amz-Expires"))
                .body(containsString("X-Amz-Credential"))
                .body(containsString("X-Amz-Signature"));
    }
}
