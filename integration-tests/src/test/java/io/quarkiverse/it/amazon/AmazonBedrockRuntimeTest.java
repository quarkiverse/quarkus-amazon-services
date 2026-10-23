package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class AmazonBedrockRuntimeTest {

    private static final String MODEL_ID = "anthropic.claude-3-haiku-20240307-v1:0";
    private static final String HTTP2_NOT_SUPPORTED = "HTTP/2 is not supported for sync HTTP clients. Either use HTTP/1.1 (the default) or use an async HTTP client (e.g., AwsCrtAsyncHttpClient).";

    @Test
    void testConverseAsync() {
        RestAssured.given().queryParam("prompt", "Hello").when()
                .get("/test/bedrockruntime/async/converse")
                .then().statusCode(200).body(startsWith("end_turn:"), containsString(MODEL_ID));
    }

    @Test
    void testInvokeModelAsync() {
        RestAssured.given().queryParam("prompt", "Hello").when()
                .get("/test/bedrockruntime/async/invoke")
                .then().statusCode(200)
                .body("role", is("assistant"))
                .body("model", is(MODEL_ID))
                .body("stop_reason", is("end_turn"))
                .body("content[0].type", is("text"));
    }

    @Test
    void testConverseStreamAsync() {
        RestAssured.given().queryParam("prompt", "Hello").when()
                .get("/test/bedrockruntime/async/converse-stream")
                .then().statusCode(200).body(startsWith("Floci stub response"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "converse", "invoke" })
    void testSync(String operation) {
        String syncClientType = System.getProperty("sync-client-type", "");

        if ("aws-crt".equals(syncClientType)) {
            // AWS CRT doesn't support HTTP/2
            RestAssured.given().pathParam("operation", operation).queryParam("prompt", "Hello").when()
                    .get("/test/bedrockruntime/sync/{operation}")
                    .then().statusCode(500).body(containsString(HTTP2_NOT_SUPPORTED));
        } else if ("converse".equals(operation)) {
            RestAssured.given().queryParam("prompt", "Hello").when()
                    .get("/test/bedrockruntime/sync/converse")
                    .then().statusCode(200).body(startsWith("end_turn:"), containsString(MODEL_ID));
        } else {
            RestAssured.given().queryParam("prompt", "Hello").when()
                    .get("/test/bedrockruntime/sync/invoke")
                    .then().statusCode(200)
                    .body("role", is("assistant"))
                    .body("model", is(MODEL_ID))
                    .body("stop_reason", is("end_turn"))
                    .body("content[0].type", is("text"));
        }
    }
}
