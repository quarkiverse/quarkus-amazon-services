package io.quarkiverse.it.amazon;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AmazonSqsMessagingConnectorTest {

    private final static String QUEUE_NAME = "quarkus-messaging-test-queue";
    private final static List<String> MESSAGES = new ArrayList<>();

    static {
        MESSAGES.add("First Message");
        MESSAGES.add("Second Message");
        MESSAGES.add("Third Message");
    }

    @Test
    public void testReceiveMessages() {
        //Publish messages
        MESSAGES.forEach(msg -> {
            given()
                    .pathParam("queueName", QUEUE_NAME)
                    .queryParam("message", msg)
                    .when().post("/test/sqs-messaging-connector/messages/{queueName}")
                    .then().body(any(String.class));
        });

        await()
                .atMost(Duration.ofSeconds(10L))
                .pollInterval(Duration.ofSeconds(1L))
                .untilAsserted(() -> assertThat(given()
                        .pathParam("queueName", QUEUE_NAME)
                        .get("/test/sqs-messaging-connector/messages/{queueName}")
                        .then()
                        .assertThat()
                        .statusCode(is(Response.Status.OK.getStatusCode()))
                        .extract()
                        .as(String[].class), arrayWithSize(3)));
    }
}
