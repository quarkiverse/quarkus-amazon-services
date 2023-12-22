package io.quarkus.it.amazon;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
public class AmazonOpenTelemetryTest {

    @BeforeEach
    @AfterEach
    void reset() {
        RestAssured.when().get("/test/reset").then().statusCode(HTTP_OK);
        await().atMost(30, SECONDS).until(() -> {
            // make sure spans are cleared
            List<Map<String, Object>> spans = getSpans();
            if (spans.size() > 0) {
                RestAssured.when().get("/test/reset").then().statusCode(HTTP_OK);
            }
            return spans.size() == 0;
        });
    }

    private List<Map<String, Object>> getSpans() {
        return RestAssured.when().get("/test/export").body().as(new TypeRef<>() {
        });
    }

    @Test
    public void testOpenTelemetryAsync() {
        RestAssured.when().get("/test/s3/async");

        assertSpans();
    }

    @Test
    public void testOpenTelemetryBlocking() {
        RestAssured.when().get("/test/s3/blocking");

        assertSpans();
    }

    private void assertSpans() {
        Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertFalse(getSpans().isEmpty());

            // Assert insert has been traced
            boolean spanEmitted = false;
            for (Map<String, Object> spanData : getSpans()) {
                if (spanData.get("instrumentationLibraryInfo") instanceof Map) {
                    final Map instrumentationLibraryInfo = (Map) spanData.get("instrumentationLibraryInfo");
                    var name = instrumentationLibraryInfo.get("name");
                    if ("io.opentelemetry.aws-sdk-2.2".equals(name)) {
                        spanEmitted = true;
                        break;
                    }
                }
            }
            assertTrue(spanEmitted, "aws sdk was not traced.");
        });
    }
}
