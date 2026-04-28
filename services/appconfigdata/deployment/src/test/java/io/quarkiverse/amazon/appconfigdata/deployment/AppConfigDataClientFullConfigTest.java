package io.quarkiverse.amazon.appconfigdata.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataAsyncClient;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;

public class AppConfigDataClientFullConfigTest {

    @Inject
    AppConfigDataClient client;

    @Inject
    AppConfigDataAsyncClient async;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("full-config.properties", "application.properties"));

    @Test
    public void test() {
        Assertions.assertNotNull(client);
        Assertions.assertNotNull(async);
    }
}
