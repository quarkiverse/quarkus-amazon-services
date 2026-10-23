package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class AmazonBedrockTest {

    @ParameterizedTest
    @ValueSource(strings = { "sync", "async" })
    void testGuardrailLifecycle(String endpoint) {
        String name = "quarkus-guardrail-" + endpoint;

        RestAssured.given().pathParam("endpoint", endpoint).queryParam("name", name).when()
                .get("/test/bedrock/{endpoint}/guardrails")
                .then().statusCode(200)
                .body("name", is(name))
                .body("version", is("1"))
                .body("status", is("READY"))
                .body("blockedInputMessaging", is("Sorry, this input is not allowed."))
                .body("contentFilters", contains("HATE"))
                .body("versions", containsInAnyOrder("DRAFT", "1"))
                .body("deleted", is(true));
    }
}
