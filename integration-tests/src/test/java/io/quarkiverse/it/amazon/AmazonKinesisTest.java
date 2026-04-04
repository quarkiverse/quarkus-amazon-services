package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Kinesis requires an HTTP/2 client, and AWS CRT doesn't support it.
 * The producer will attempt to create both synchronous and asynchronous clients, and it will fail with the AWS CRT client.
 */
@QuarkusTest
public class AmazonKinesisTest {

    @Test
    public void testKinesisAsync() {
        RestAssured.when().get("/test/kinesis/async").then()
                .body(is("arn:aws:kinesis:us-east-1:000000000000:stream/quarkus-stream-async"));
    }

    @Test
    public void testKinesisSync() {
        String syncClientType = System.getProperty("sync-client-type", "");

        if ("aws-crt".equals(syncClientType)) {
            // AWS CRT doesn't support HTTP/2
            RestAssured.when().get("/test/kinesis/sync").then()
                    .body(containsString(
                            "HTTP/2 is not supported for sync HTTP clients. Either use HTTP/1.1 (the default) or use an async HTTP client (e.g., AwsCrtAsyncHttpClient)."));
        } else {
            // Other sync clients should work fine
            RestAssured.when().get("/test/kinesis/sync").then()
                    .body(is("arn:aws:kinesis:us-east-1:000000000000:stream/quarkus-stream-sync"));
        }
    }
}
