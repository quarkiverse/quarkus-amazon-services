package io.quarkiverse.it.amazon;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class SecretsManagerConfigPollingTest {
    @Test
    void refreshesStoredSecretValues() {
        given().get("/test/configuration/secret-polling").then().statusCode(200);
        String updated = "updated-" + UUID.randomUUID();
        given().body(updated).put("/test/configuration/secret-polling").then().statusCode(204);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> given()
                .get("/test/configuration/secret-polling").then().statusCode(200).body(is(updated)));
    }
}
