package io.quarkiverse.it.amazon;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonSecretsManagerTest {
    @Test
    public void expressionsAreRuntimeOnlyAndDynamicReadsSeeUpdatesUnlikeInitialInjection() {
        RestAssured.when().get("/test/secretsmanager/expressions").then().statusCode(200).body(is("initial|initial|localhost"));
        RestAssured.given().contentType(TEXT_PLAIN).body("rotated").put("/test/secretsmanager/expressions").then()
                .statusCode(204);
        RestAssured.when().get("/test/secretsmanager/expressions").then().statusCode(200).body(is("initial|rotated|localhost"));
    }

    @Test
    public void testSecretsManagerAsync() {
        RestAssured.when().get("/test/secretsmanager/async").then().body(is("Quarkus is awesome"));
    }

    @Test
    public void testSecretsManagerSync() {
        RestAssured.when().get("/test/secretsmanager/sync").then().body(is("Quarkus is awesome"));
    }

    @Test
    public void testSecretsManagerConfigSource() {
        RestAssured.when().get("/test/secretsmanager/config").then().body(is(
                "postgresUsername: quarkus, postgresPassword: quarkus, postgresUrl: jdbc:postgresql://localhost:5432/quarkus"));
    }

    @Test
    public void testSecretsManagerJsonConfigSource() {
        RestAssured.when().get("/test/secretsmanager/config-json").then()
                .body(is("db.host: localhost, db.port: 5432"));
    }
}
