package io.quarkiverse.it.amazon;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@EnabledIfSystemProperty(named = "without-yaml", matches = "true")
public class AwsConfigurationWithoutYamlTest {
    @Test
    void nonYamlFormatsAndAllFourSourcesWorkWithoutYaml() {
        given().get("/test/configuration/without-yaml").then().statusCode(200)
                .body("raw", is("raw"), "json", is("json"), "properties", is("properties"),
                        "yaml", is("YAML configuration requires the quarkus-config-yaml extension."),
                        "s3", is("initial"), "ssm", is("localhost"), "secretsmanager", is("localhost"),
                        "appconfig", is("my-appconfigdata-application"));
    }
}
