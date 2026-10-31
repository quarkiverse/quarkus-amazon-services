package io.quarkiverse.amazon.sdk.deployment;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class SdkAsyncClientNoPoolVertxContextTest extends AbstractSdkAsyncClientVertxContextTest {
    @RegisterExtension
    static final QuarkusExtensionTest app = configure()
            .overrideConfigKey("quarkus.aws.async-client.type", "netty")
            .overrideConfigKey("quarkus.aws.async-client.advanced.use-future-completion-thread-pool", "false");
}
