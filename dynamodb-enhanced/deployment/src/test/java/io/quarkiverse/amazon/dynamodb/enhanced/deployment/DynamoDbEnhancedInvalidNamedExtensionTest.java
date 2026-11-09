package io.quarkiverse.amazon.dynamodb.enhanced.deployment;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

class DynamoDbEnhancedInvalidNamedExtensionTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.dynamodbenhanced.\"invalid\".client-extensions", "not.an.Extension")
            .assertException(failure -> {
                while (failure.getCause() != null) {
                    failure = failure.getCause();
                }
                assertTrue(failure.getMessage().contains("quarkus.dynamodbenhanced.\"invalid\".client-extensions"));
                assertTrue(failure.getMessage().contains("not.an.Extension"));
            });

    @Test
    void invalidNamedExtensionsFailAtBuildTime() {
        fail("Deployment should have failed");
    }
}
