package io.quarkus.amazon.sts.deployment;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.sts.StsAsyncClient;
import software.amazon.awssdk.services.sts.StsClient;

public class StsSyncClientFullConfigTest {

    @Inject
    StsClient client;

    @Inject
    StsAsyncClient async;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot(
            (jar) -> jar.addAsResource("full-config.properties", "application.properties"));

    @Test
    public void test() {
        // should finish with success
    }
}
