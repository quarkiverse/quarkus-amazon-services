package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonSesTest {

    @Test
    public void testSesAsync() {
        RestAssured.when().get("/test/ses/async").then().body(any(String.class), not(emptyString()));
    }

    @Test
    public void testSesSync() {
        RestAssured.when().get("/test/ses/sync").then().body(any(String.class), not(emptyString()));
    }

    /**
     * Replays what the SES Dev Services processor does when it reuses a shared container whose resources already
     * exist: creating them again must not fail. The processor code is not called, only its behavior is reproduced.
     */
    @Test
    public void testDevServicesIdentityReplay() {
        RestAssured.when().get("/test/ses/devservices-identity-replay").then().body(is("OK"));
    }
}
