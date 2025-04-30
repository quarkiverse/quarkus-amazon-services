package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;

@QuarkusTest
class AmazonBedrockRuntimeTest {

    @ParameterizedTest
    @ValueSource(strings = { "sync", "async" })
    void testPublishAndReceive(String endpoint) {
        RestAssured.defaultParser = Parser.JSON;

        // List invoked
        RestAssured.given().pathParam("endpoint", endpoint).when()
                .get("/test/bedrockruntime/{endpoint}/list-invokes")
                // test at least for pro feature failure
                .then().statusCode(oneOf(501, 500)).body(anyOf(is(""), containsString(
                        "HTTP/2 is not supported in AwsCrtHttpClient yet. Use NettyNioAsyncHttpClient instead")));
    }
}
