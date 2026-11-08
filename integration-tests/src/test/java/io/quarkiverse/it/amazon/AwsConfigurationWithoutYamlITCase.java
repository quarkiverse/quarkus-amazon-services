package io.quarkiverse.it.amazon;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
@EnabledIfSystemProperty(named = "without-yaml", matches = "true")
public class AwsConfigurationWithoutYamlITCase extends AwsConfigurationWithoutYamlTest {
}
