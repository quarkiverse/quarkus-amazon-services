package io.quarkiverse.amazon.fake.deployment;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class FakeCrtAsyncClientVertxContextTest extends AbstractFakeAsyncClientVertxContextTest {
    @RegisterExtension
    static final QuarkusExtensionTest app = configure("aws-crt");
}
