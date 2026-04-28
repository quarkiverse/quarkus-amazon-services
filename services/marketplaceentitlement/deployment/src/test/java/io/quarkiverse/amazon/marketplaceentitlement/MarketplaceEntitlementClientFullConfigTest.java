package io.quarkiverse.amazon.marketplaceentitlement;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.services.marketplaceentitlement.MarketplaceEntitlementAsyncClient;
import software.amazon.awssdk.services.marketplaceentitlement.MarketplaceEntitlementClient;

class MarketplaceEntitlementClientFullConfigTest {

    @Inject
    MarketplaceEntitlementClient client;

    @Inject
    MarketplaceEntitlementAsyncClient async;

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
