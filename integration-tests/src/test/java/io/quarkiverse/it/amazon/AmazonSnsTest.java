package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.time.Duration;
import java.util.concurrent.Callable;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;

@QuarkusTest
public class AmazonSnsTest {

    private final static String TOPIC_NAME = "quarkus-sns";

    @BeforeEach
    public void before() {
        RestAssured.given().pathParam("topicName", TOPIC_NAME).post("/test/sns/topics/{topicName}");
    }

    @AfterEach
    public void after() {
        RestAssured.given().pathParam("topicName", TOPIC_NAME).delete("/test/sns/topics/{topicName}");
    }

    @ParameterizedTest
    @ValueSource(strings = { "sync", "async" })
    public void testPublishAndReceive(String endpoint) {
        String message = "Quarkus is awesome";
        //Publish message
        RestAssured.given()
                .pathParam("endpoint", endpoint)
                .pathParam("topicName", TOPIC_NAME)
                .queryParam("message", message)
                .when().post("/test/sns/{endpoint}/publish/{topicName}")
                .then().body(any(String.class));

        Awaitility.await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofSeconds(1))
                .until(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Response response = RestAssured.given()
                                .pathParam("topicName", TOPIC_NAME)
                                .when().get("/test/sns/topics/{topicName}").andReturn();
                        return response.getBody().asString().contains(message);
                    }
                });
    }

    /**
     * The stack container must reach the application through host.docker.internal, which Linux does not resolve
     * unless Dev Services maps it.
     */
    @Test
    public void testHttpSubscriptionReachesApplication() {
        RestAssured.given()
                .pathParam("topicName", TOPIC_NAME)
                .queryParam("endpoint",
                        "http://host.docker.internal:" + RestAssured.port + "/test/sns/http-notifications")
                .when().post("/test/sns/topics/{topicName}/http-subscriptions")
                .then().statusCode(200);

        Awaitility.await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> RestAssured.get("/test/sns/http-notifications")
                        .then().body(containsString("SubscriptionConfirmation")));
    }

    /**
     * Replays what the SNS Dev Services processor does when it reuses a shared container whose resources already
     * exist: creating them again must not fail. The processor code is not called, only its behavior is reproduced.
     */
    @Test
    public void testDevServicesTopicReplay() {
        RestAssured.when().get("/test/sns/devservices-topic-replay").then().body(is("OK"));
    }
}
