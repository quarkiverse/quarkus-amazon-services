package io.quarkus.it.amazon;

import static org.hamcrest.Matchers.any;

import java.time.Duration;
import java.util.concurrent.Callable;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
}
