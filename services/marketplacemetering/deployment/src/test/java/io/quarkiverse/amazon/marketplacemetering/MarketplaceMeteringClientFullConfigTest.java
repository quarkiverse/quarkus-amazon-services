package io.quarkiverse.amazon.marketplacemetering;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.marketplacemetering.MarketplaceMeteringAsyncClient;
import software.amazon.awssdk.services.marketplacemetering.MarketplaceMeteringClient;

class MarketplaceMeteringClientFullConfigTest {

    @Inject
    MarketplaceMeteringClient client;

    @Inject
    MarketplaceMeteringAsyncClient async;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("full-config.properties", "application.properties"));

    @Test
    void test() {
        Assertions.assertNotNull(client);
        Assertions.assertNotNull(async);
    }
}
