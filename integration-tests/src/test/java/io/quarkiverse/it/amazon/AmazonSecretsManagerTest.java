package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonSecretsManagerTest {

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
