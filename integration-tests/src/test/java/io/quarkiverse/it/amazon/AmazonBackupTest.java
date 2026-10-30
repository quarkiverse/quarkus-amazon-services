package io.quarkiverse.it.amazon;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AmazonBackupTest {
    @Test
    public void syncClients() {
        roundTrip("sync");
    }

    @Test
    public void asyncClients() {
        roundTrip("async");
    }

    private void roundTrip(String mode) {
        String name = "generic-" + UUID.randomUUID();
        try {
            given().post("/test/backup/" + mode + "/" + name).then().statusCode(200).body(is(name));
            given().get("/test/backup/" + mode + "/" + name).then().statusCode(200).body(is(name));
        } finally {
            given().delete("/test/backup/" + name).then().statusCode(204);
        }
    }
}
