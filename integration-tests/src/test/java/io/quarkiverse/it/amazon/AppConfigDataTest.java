package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AppConfigDataTest {

    @Test
    public void testAppConfigDataConfigSource() {
        RestAssured.when().get("/test/appconfigdata/config").then()
                .body(is("application: my-appconfigdata-application"));
    }
}
