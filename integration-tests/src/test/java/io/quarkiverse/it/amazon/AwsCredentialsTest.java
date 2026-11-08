package io.quarkiverse.it.amazon;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AwsCredentialsTest {
    @Test
    public void documentAndBatchCredentialsReadRotatedValues() {
        for (String service : new String[] { "sm", "ssm" }) {
            for (String password : new String[] { "initial-fixture", "rotated-fixture" }) {
                given().contentType("text/plain").body(password)
                        .put("/test/configuration/credentials/" + service).then().statusCode(204);
                for (String selection : new String[] { "document", "batch" }) {
                    given().get("/test/configuration/credentials/" + service + "/" + selection).then()
                            .statusCode(200).body("user", is("fixture-user"), "password", is(password));
                }
            }
        }
    }
}
