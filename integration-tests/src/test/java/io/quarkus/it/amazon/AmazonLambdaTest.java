package io.quarkus.it.amazon;

import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.it.amazon.lambda.LambdaResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonLambdaTest {

    byte[] helloLambdaZipArchive;

    @BeforeEach
    public void setup() throws IOException {
        if (ArrayUtils.isEmpty(helloLambdaZipArchive)) {
            helloLambdaZipArchive = IOUtils
                    .toByteArray(Optional.ofNullable(getClass().getResourceAsStream("/functions/hello-lambda.zip-archive"))
                            .orElseThrow());
        }
    }

    @Test
    public void testAsync() {
        RestAssured
                .given()
                .when()
                .body(helloLambdaZipArchive)
                .post("/test/lambda/async")
                .then()
                .body(is(LambdaResource.OK));
    }

    @Test
    public void testBlocking() {
        RestAssured
                .given()
                .when()
                .body(helloLambdaZipArchive)
                .post("/test/lambda/blocking").then()
                .body(is(LambdaResource.OK));
    }

}
