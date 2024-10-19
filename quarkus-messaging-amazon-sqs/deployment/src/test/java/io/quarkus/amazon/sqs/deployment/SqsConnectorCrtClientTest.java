package io.quarkus.amazon.sqs.deployment;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;

import jakarta.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class SqsConnectorCrtClientTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(Quote.class)
                    .addClass(QuoteProcessor.class)
                    .addClass(QuotesResource.class)
                    .addAsResource(new StringAsset(
                            """
                                    quarkus.sqs.devservices.queues=quotes,quote-requests
                                    mp.messaging.incoming.requests.queue=quote-requests
                                    mp.messaging.incoming.quotes-in.queue=quotes
                                    quarkus.sqs.async-client.type=aws-crt"""),
                            "application.properties"));

    @Test
    void test() {
        given().post("/quotes/request")
                .then()
                .statusCode(200);
        given().post("/quotes/request")
                .then()
                .statusCode(200);

        await().untilAsserted(() -> {
            assertThat(given().get("/quotes")
                    .then()
                    .assertThat()
                    .statusCode(is(Response.Status.OK.getStatusCode()))
                    .extract()
                    .as(Quote[].class), arrayWithSize(2));
        });

    }
}
