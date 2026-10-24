package io.quarkiverse.amazon.ssm.deployment;

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
public class SsmConfigSourceCloserTest {

    private static final String POLL_THREAD_NAME = "quarkus-amazon-ssm-config-poll";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.ssm.config.enabled", "true")
            .overrideConfigKey("quarkus.ssm.config.path", "/app")
            .overrideConfigKey("quarkus.ssm.config.update-interval-minutes", "60")
            .overrideConfigKey("quarkus.ssm.endpoint-override", "http://localhost:1")
            .overrideConfigKey("quarkus.ssm.aws.region", "us-east-1")
            .overrideConfigKey("quarkus.ssm.aws.credentials.type", "anonymous");

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
