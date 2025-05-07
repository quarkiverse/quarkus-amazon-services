package io.quarkiverse.amazon.sso.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.sso.SsoAsyncClient;
import software.amazon.awssdk.services.sso.SsoClient;

public class SsoSyncClientFullConfigTest {

    @Inject
    SsoClient client;

    @Inject
    SsoAsyncClient async;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot(
            (jar) -> jar.addAsResource("full-config.properties", "application.properties"));

    @Test
    public void test() {
        // should finish with success
    }
}
