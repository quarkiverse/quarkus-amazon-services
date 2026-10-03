package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.is;

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
        RestAssured.when().get("/test/ssm/config").then().body(is(
                "postgresUsername: quarkus, postgresPassword: quarkus, postgresUrl: jdbc:postgresql://localhost:5432/quarkus"));
    }

    @Test
    public void testSsmJsonConfigSource() {
        RestAssured.when().get("/test/ssm/config-json").then()
                .body(is("db1.host: localhost, db1.port: 5432, db2.host: localhost, db2.port: 5433"));
    }
}
