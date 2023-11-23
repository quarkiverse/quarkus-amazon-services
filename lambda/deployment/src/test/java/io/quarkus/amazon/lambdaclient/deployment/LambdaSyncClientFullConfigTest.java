package io.quarkus.amazon.lambdaclient.deployment;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.lambda.LambdaClient;

public class LambdaSyncClientFullConfigTest {

    @Inject
    Instance<LambdaClient> client;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("sync-urlconn-full-config.properties", "application.properties"));

    @Test
    public void test() {
        assertNotNull(client.get());
        // should finish with success
    }
}
