package io.quarkus.it.amazon;

import static org.hamcrest.Matchers.anyOf;
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
                .body(anyOf(is("arn:aws:kinesis:us-east-1:000000000000:stream/quarkus-stream-async"), containsString(
                        "HTTP/2 is not supported in AwsCrtHttpClient yet. Use NettyNioAsyncHttpClient instead")));
    }

    @Test
    public void testKinesisSync() {
        RestAssured.when().get("/test/kinesis/sync").then()
                .body(anyOf(is("arn:aws:kinesis:us-east-1:000000000000:stream/quarkus-stream-sync"), containsString(
                        "HTTP/2 is not supported in AwsCrtHttpClient yet. Use NettyNioAsyncHttpClient instead")));
    }
}
