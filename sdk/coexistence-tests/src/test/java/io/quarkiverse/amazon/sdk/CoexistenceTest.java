package io.quarkiverse.amazon.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkus.test.junit.QuarkusTest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.sesv2.SesV2AsyncClient;
import software.amazon.awssdk.services.sesv2.SesV2Client;

@QuarkusTest
class CoexistenceTest {
    @Inject
    EcrClient generic;

    @Inject
    @AmazonClient("archive")
    EcrClient namedGeneric;

    @Inject
    SesV2Client dedicated;

    @Inject
    @AmazonClient("archive")
    SesV2Client namedDedicated;

    @Inject
    @AmazonClient("archive")
    SesV2AsyncClient asyncDedicated;

    @Test
    void dedicatedClientsKeepTheirConfigurationAndUnclaimedClientsUseTheGenericExtension() {
        assertEquals(Region.EU_WEST_1, generic.serviceClientConfiguration().region());
        assertEquals(Region.AP_SOUTH_1, namedGeneric.serviceClientConfiguration().region());
        assertEquals(Region.EU_CENTRAL_1, dedicated.serviceClientConfiguration().region());
        assertEquals(Region.US_WEST_2, namedDedicated.serviceClientConfiguration().region());
        assertEquals(Region.US_WEST_2, asyncDedicated.serviceClientConfiguration().region());
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        String prefix = "io/quarkiverse/amazon/sdk/runtime/generated/software/amazon/awssdk/services/";
        assertNotNull(loader.getResource(prefix + "ecr/EcrClientBuilderFactory.class"));
        assertNull(loader.getResource(prefix + "sesv2/SesV2ClientBuilderFactory.class"));
        assertNull(loader.getResource(prefix + "sesv2/SesV2AsyncClientBuilderFactory.class"));
    }
}
