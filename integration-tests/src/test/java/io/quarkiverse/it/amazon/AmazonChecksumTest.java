package io.quarkiverse.it.amazon;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonChecksumTest {

    @Test
    public void testChecksumAsync() {
        RestAssured.when().get("/test/checksum/create").then().body(is("ok:10"));
    }
}
