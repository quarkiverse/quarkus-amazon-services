package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.is;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonCloudWatchLogTest {

    String prefix = "/test/cloudwatchlogs";

    @Test
    @Timeout(30)
    public void testSync() {
        UUID logGroup = UUID.randomUUID();
        UUID logStream = UUID.randomUUID();
        UUID message = UUID.randomUUID();

        String sync = "sync";

        RestAssured.given()
                .formParam("logGroup", logGroup.toString())
                .formParam("logStream", logStream.toString())
                .formParam("message", message.toString())
                .post(prefix + "/" + sync + "/put")
                .then().statusCode(200)
                .body(is("OK"));

        RestAssured.given()
                .queryParam("logGroup", logGroup.toString())
                .queryParam("logStream", logStream.toString())
                .get(prefix + "/" + sync + "/get")
                .then().statusCode(200)
                .body(is(message.toString()));
    }

    @Test
    @Timeout(30)
    public void testAsync() {
        UUID logGroup = UUID.randomUUID();
        UUID logStream = UUID.randomUUID();
        UUID message = UUID.randomUUID();

        String async = "async";

        RestAssured.given()
                .formParam("logGroup", logGroup.toString())
                .formParam("logStream", logStream.toString())
                .formParam("message", message.toString())
                .post(prefix + "/" + async + "/put")
                .then().statusCode(200)
                .body(is("OK"));

        RestAssured.given()
                .queryParam("logGroup", logGroup.toString())
                .queryParam("logStream", logStream.toString())
                .get(prefix + "/" + async + "/get")
                .then().statusCode(200)
                .body(is(message.toString()));
    }
}
