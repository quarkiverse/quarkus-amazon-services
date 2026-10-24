package io.quarkiverse.amazon.appconfigdata.deployment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * The config source poller is stopped on shutdown. The endpoint is unreachable, so loading fails, but the poller
 * still starts.
 */
public class AppConfigDataConfigSourceCloserTest {

    private static final String POLL_THREAD_NAME = "quarkus-amazon-appconfigdata-config-poll";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.appconfigdata.config.enabled", "true")
            .overrideConfigKey("quarkus.appconfigdata.config.application", "app")
            .overrideConfigKey("quarkus.appconfigdata.config.environment", "env")
            .overrideConfigKey("quarkus.appconfigdata.config.configuration-profile", "profile")
            .overrideConfigKey("quarkus.appconfigdata.config.update-interval-minutes", "60")
            .overrideConfigKey("quarkus.appconfigdata.endpoint-override", "http://localhost:1")
            .overrideConfigKey("quarkus.appconfigdata.aws.region", "us-east-1")
            .overrideConfigKey("quarkus.appconfigdata.aws.credentials.type", "anonymous");

    @Test
    public void test() throws InterruptedException {
        Optional<Thread> poller = Thread.getAllStackTraces().keySet().stream()
                .filter(t -> t.getName().equals(POLL_THREAD_NAME))
                .findFirst();
        assertTrue(poller.isPresent());

        Arc.container().beanManager().getEvent().fire(new ShutdownEvent());

        poller.get().join(5000);
        assertFalse(poller.get().isAlive());
    }
}
