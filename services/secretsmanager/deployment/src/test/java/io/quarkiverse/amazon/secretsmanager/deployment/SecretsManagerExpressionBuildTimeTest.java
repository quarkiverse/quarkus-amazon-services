package io.quarkiverse.amazon.secretsmanager.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.test.QuarkusExtensionTest;

class SecretsManagerExpressionBuildTimeTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.secretsmanager.endpoint-override", "http://localhost:1")
            .overrideConfigKey("quarkus.secretsmanager.aws.region", "us-east-1")
            .overrideConfigKey("quarkus.secretsmanager.aws.credentials.type", "anonymous")
            .overrideConfigKey("quarkus.secretsmanager.api-call-timeout", "100ms")
            .overrideConfigKey("expression.value", "${sm::must-not-fetch-at-build}")
            .addBuildChainCustomizer(builder -> builder.addBuildStep(context -> {
                var failure = assertThrows(RuntimeException.class,
                        () -> ConfigProvider.getConfig().getValue("expression.value", String.class));
                assertTrue(failure.getMessage().contains("handler"));
                assertTrue(failure.getMessage().contains("sm"));
                context.produce(new GeneratedResourceBuildItem("sm-expression-build-checked", new byte[0]));
            }).produces(GeneratedResourceBuildItem.class).build());

    @Test
    void handlerIsAbsentDuringAugmentationButAvailableAtRuntime() {
        assertNotNull(Thread.currentThread().getContextClassLoader().getResource("sm-expression-build-checked"));
        var failure = assertThrows(IllegalStateException.class,
                () -> ConfigProvider.getConfig().getValue("expression.value", String.class));
        assertEquals("Unable to resolve AWS sm expression; check resource, permissions, content and client settings.",
                failure.getMessage());
    }
}
