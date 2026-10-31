package io.quarkiverse.amazon.fake.deployment;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class FakeAsyncClientNoPoolVertxContextTest extends AbstractFakeAsyncClientVertxContextTest {
    @RegisterExtension
    static final QuarkusExtensionTest app = configure("netty")
            .overrideConfigKey("quarkus.fake-service.async-client.advanced.use-future-completion-thread-pool", "false");
}
