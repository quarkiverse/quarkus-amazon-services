package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.is;

import java.time.Duration;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AppConfigDataTest {

    @Test
    public void testAppNameConfig() {
        RestAssured.when().get("/test/appconfigdata/app-name").then()
                .body(is("application: my-appconfigdata-application"));
    }

    @Test
    void testAppConfigDataPolling() {
        RestAssured.with()
                .body("{\"application\": \"my-appconfigdata-application\", \"host\": \"example.com\", \"port\": 1234}")
                .put("/test/appconfigdata/config");

        RestAssured.get("/test/appconfigdata/db-config")
                .then()
                .body(is("host: localhost, port: 5432"));

        Awaitility.await().atMost(Duration.ofMinutes(2L))
                .untilAsserted(() -> RestAssured.when()
                        .get("/test/appconfigdata/db-config")
                        .then()
                        .body(is("host: example.com, port: 1234")));
    }
}
