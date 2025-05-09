package io.quarkiverse.amazon.ssooidc.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.ssooidc.SsoOidcAsyncClient;
import software.amazon.awssdk.services.ssooidc.SsoOidcClient;

public class SsoOidcSyncClientFullConfigTest {

    @Inject
    SsoOidcClient client;

    @Inject
    SsoOidcAsyncClient async;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot(
            (jar) -> jar.addAsResource("full-config.properties", "application.properties"));

    @Test
    public void test() {
        // should finish with success
    }
}
