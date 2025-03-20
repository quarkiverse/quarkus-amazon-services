package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.any;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;

@QuarkusTest
class AmazonBedrockTest {

    @ParameterizedTest
    @ValueSource(strings = { "sync", "async" })
    void testPublishAndReceive(String endpoint) {
        RestAssured.defaultParser = Parser.JSON;

        // List invoked
        RestAssured.given().pathParam("endpoint", endpoint).when()
                .get("/test/bedrock/{endpoint}/foundation-models")
                // test at least for pro feature failure
                .then().statusCode(501).body(any(String.class));
    }
}
