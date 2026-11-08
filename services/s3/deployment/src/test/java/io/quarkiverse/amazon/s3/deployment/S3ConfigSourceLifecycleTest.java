package io.quarkiverse.amazon.s3.deployment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.test.QuarkusExtensionTest;

class S3ConfigSourceLifecycleTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.s3.config.enabled", "true")
            .overrideConfigKey("quarkus.s3.config.fail-on-missing-config", "false")
            .overrideConfigKey("quarkus.s3.config.entries.app.bucket", "bucket")
            .overrideConfigKey("quarkus.s3.config.entries.app.key", "application.properties")
            .overrideConfigKey("quarkus.s3.config.update-interval", "1H")
            .overrideConfigKey("quarkus.s3.endpoint-override", "http://localhost:1")
            .overrideConfigKey("quarkus.s3.aws.region", "us-east-1")
            .overrideConfigKey("quarkus.s3.aws.credentials.type", "anonymous")
            .overrideConfigKey("quarkus.s3.api-call-timeout", "100ms")
            .addBuildChainCustomizer(builder -> builder.addBuildStep(context -> {
                assertFalse(hasS3Source());
                context.produce(new GeneratedResourceBuildItem("s3-config-build-checked", new byte[0]));
            }).produces(GeneratedResourceBuildItem.class).build());

    @Test
    void sourceIsRuntimeOnlyAndShutdownStopsItsPoller() throws InterruptedException {
        assertTrue(hasS3Source());
        assertTrue(Thread.currentThread().getContextClassLoader().getResource("s3-config-build-checked") != null);
        var poller = Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> thread.getName().equals("quarkus-amazon-s3-config-poll")).findFirst().orElseThrow();
        Arc.container().beanManager().getEvent().fire(new ShutdownEvent());
        poller.join(5000);
        assertFalse(poller.isAlive());
    }

    private static boolean hasS3Source() {
        for (var source : ConfigProvider.getConfig().getConfigSources()) {
            if (source.getName().equals("quarkus.s3.config")) {
                return true;
            }
        }
        return false;
    }
}
