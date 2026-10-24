package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonSesV2Test {

    @Test
    public void testSesV2Async() {
        RestAssured.when().get("/test/sesv2/async").then().statusCode(200).body(not(emptyString()));
    }

    @Test
    public void testSesV2Sync() {
        RestAssured.when().get("/test/sesv2/sync").then().statusCode(200).body(not(emptyString()));
    }

    /**
     * Replays what the SES v2 Dev Services processor does when it reuses a shared container whose resources already
     * exist: creating them again must not fail. The processor code is not called, only its behavior is reproduced.
     */
    @Test
    public void testDevServicesIdentityReplay() {
        RestAssured.when().get("/test/sesv2/devservices-identity-replay").then().body(is("OK"));
    }
}
