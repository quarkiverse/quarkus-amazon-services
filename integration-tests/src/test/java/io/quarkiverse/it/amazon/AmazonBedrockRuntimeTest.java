package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;

@QuarkusTest
class AmazonBedrockRuntimeTest {

    @Test
    void testPublishAndReceiveAsync() {
        RestAssured.defaultParser = Parser.JSON;

        // List invoked
        RestAssured.given().pathParam("endpoint", "async").when()
                .get("/test/bedrockruntime/{endpoint}/list-invokes")
                // test at least for pro feature failure
                .then().statusCode(oneOf(501)).body(is(""));
    }

    @Test
    void testPublishAndReceiveSync() {
        RestAssured.defaultParser = Parser.JSON;
        String syncClientType = System.getProperty("sync-client-type", "");

        if ("aws-crt".equals(syncClientType)) {
            // AWS CRT doesn't support HTTP/2
            RestAssured.given().pathParam("endpoint", "sync").when()
                    .get("/test/bedrockruntime/{endpoint}/list-invokes")
                    .then().statusCode(500)
                    .body(containsString(
                            "HTTP/2 is not supported for sync HTTP clients. Either use HTTP/1.1 (the default) or use an async HTTP client (e.g., AwsCrtAsyncHttpClient)."));
        } else {
            // Other sync clients should work fine
            RestAssured.given().pathParam("endpoint", "sync").when()
                    .get("/test/bedrockruntime/{endpoint}/list-invokes")
                    .then().statusCode(501).body(is(""));
        }
    }
}
