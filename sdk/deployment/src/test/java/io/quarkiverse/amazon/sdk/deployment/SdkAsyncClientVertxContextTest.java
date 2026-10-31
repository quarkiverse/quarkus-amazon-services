package io.quarkiverse.amazon.sdk.deployment;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class SdkAsyncClientVertxContextTest extends AbstractSdkAsyncClientVertxContextTest {
    @RegisterExtension
    static final QuarkusExtensionTest app = configure()
            .overrideConfigKey("quarkus.aws.async-client.type", "netty");
}
