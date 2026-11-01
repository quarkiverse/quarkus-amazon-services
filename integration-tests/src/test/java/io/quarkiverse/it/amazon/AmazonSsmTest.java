package io.quarkiverse.it.amazon;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.hamcrest.Matchers.is;

import java.time.Duration;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonSsmTest {

    @Test
    public void testSsmAsync() {
        RestAssured.when().get("/test/ssm/async").then().body(is("Quarkus is awesome"));
    }

    @Test
    public void testSsmSync() {
        RestAssured.when().get("/test/ssm/sync").then().body(is("Quarkus is awesome"));
    }

    @Test
    public void testSsmConfigSource() {
        RestAssured.when()
                .get("/test/ssm/config")
                .then()
                .body(is(
                        "postgresUsername: quarkus, postgresPassword: quarkus, postgresUrl: jdbc:postgresql://localhost:5432/quarkus"));
    }

    @Test
    public void testSsmJsonConfigSource() {
        RestAssured.when()
                .get("/test/ssm/config-json")
                .then()
                .body(is("db1.host: localhost, db1.port: 5432"));
    }

    @Test
    public void testSsmPolling() {
        RestAssured.with()
                .contentType(TEXT_PLAIN)
                .body("{\"host\": \"example.com\", \"port\": 1234 }")
                .put("/test/ssm/other-config-json");

        Awaitility.await().atMost(Duration.ofSeconds(15L)).untilAsserted(() -> {
            RestAssured.when()
                    .get("/test/ssm/other-config-json")
                    .then()
                    .body(is("db2.host: example.com, db2.port: 1234"));
        });
    }
}
