package io.quarkiverse.amazon.ssm.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.test.QuarkusExtensionTest;

class SsmExpressionBuildTimeTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.ssm.endpoint-override", "http://localhost:1")
            .overrideConfigKey("quarkus.ssm.aws.region", "us-east-1")
            .overrideConfigKey("quarkus.ssm.aws.credentials.type", "anonymous")
            .overrideConfigKey("quarkus.ssm.api-call-timeout", "100ms")
            .overrideConfigKey("expression.value", "${ssm::/must-not-fetch-at-build}")
            .addBuildChainCustomizer(builder -> builder.addBuildStep(context -> {
                var failure = assertThrows(RuntimeException.class,
                        () -> ConfigProvider.getConfig().getValue("expression.value", String.class));
                assertTrue(failure.getMessage().contains("handler"));
                assertTrue(failure.getMessage().contains("ssm"));
                context.produce(new GeneratedResourceBuildItem("ssm-expression-build-checked", new byte[0]));
            }).produces(GeneratedResourceBuildItem.class).build());

    @Test
    void handlerIsAbsentDuringAugmentationButAvailableAtRuntime() {
        assertNotNull(Thread.currentThread().getContextClassLoader().getResource("ssm-expression-build-checked"));
        var failure = assertThrows(IllegalStateException.class,
                () -> ConfigProvider.getConfig().getValue("expression.value", String.class));
        assertEquals("Unable to resolve AWS ssm expression; check resource, permissions, content and client settings.",
                failure.getMessage());
    }
}
