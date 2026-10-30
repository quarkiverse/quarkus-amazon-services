package io.quarkiverse.amazon.sdk.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.sesv2.SesV2AsyncClient;
import software.amazon.awssdk.services.sesv2.SesV2Client;

/**
 * Verifies multi-service sync and async injection, shared client names, configuration overrides, and factories for requested
 * types only.
 */
class GenericClientsTest {
    @RegisterExtension
    static final QuarkusExtensionTest app = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(GenericClientsTest.class))
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.aws.region", "eu-west-1")
            .overrideConfigKey("quarkus.aws.credentials.type", "static")
            .overrideConfigKey("quarkus.aws.credentials.static-provider.access-key-id", "global-key")
            .overrideConfigKey("quarkus.aws.credentials.static-provider.secret-access-key", "global-secret")
            .overrideConfigKey("quarkus.aws.api-call-timeout", "12s")
            .overrideConfigKey("quarkus.aws.\"backup\".region", "us-west-2")
            .overrideConfigKey("quarkus.aws.\"backup\".credentials.type", "anonymous")
            .overrideConfigKey("quarkus.aws.\"backup\".endpoint-override", "http://localhost:4566")
            .overrideConfigKey("quarkus.aws.\"backup\".api-call-timeout", "3s")
            .overrideConfigKey("quarkus.aws.\"mail\".region", "eu-central-1")
            .overrideConfigKey("quarkus.aws.\"mail\".credentials.type", "static")
            .overrideConfigKey("quarkus.aws.\"mail\".credentials.static-provider.access-key-id", "mail-key")
            .overrideConfigKey("quarkus.aws.\"mail\".credentials.static-provider.secret-access-key", "mail-secret")
            .overrideConfigKey("quarkus.aws.\"archive.eu\".region", "ap-south-1")
            .overrideConfigKey("quarkus.aws.\"region\".region", "us-east-2")
            .overrideConfigKey("quarkus.aws.\"devservices\".region", "us-east-1");

    @Inject
    EcrClient ecr;

    @Inject
    @AmazonClient("backup")
    EcrClient ecrBackup;

    @Inject
    SesV2Client ses;

    @Inject
    @AmazonClient("backup")
    SesV2Client sesBackup;

    @Inject
    @AmazonClient("unconfigured")
    SesV2Client unconfigured;

    @Inject
    @AmazonClient("mail")
    SesV2AsyncClient async;

    @Inject
    @AmazonClient("archive.eu")
    SesV2Client archive;

    @Inject
    @AmazonClient("region")
    EcrClient namedRegion;

    @Inject
    @AmazonClient("devservices")
    EcrClient namedDevServices;

    @Test
    void inheritsGlobalDefaultsAndGlobalNamedClients() {
        assertEquals(Region.EU_WEST_1, ecr.serviceClientConfiguration().region());
        assertEquals(Region.US_WEST_2, ecrBackup.serviceClientConfiguration().region());
        assertInstanceOf(AnonymousCredentialsProvider.class, ecrBackup.serviceClientConfiguration().credentialsProvider());
        assertEquals("global-key", ((AwsCredentialsProvider) ecr.serviceClientConfiguration().credentialsProvider())
                .resolveCredentials().accessKeyId());
        assertEquals(Duration.ofSeconds(12),
                ecr.serviceClientConfiguration().overrideConfiguration().apiCallTimeout().orElseThrow());
    }

    @Test
    void sharesNamesAcrossServicesAndIsolatesDifferentNames() {
        assertEquals(Region.EU_WEST_1, ses.serviceClientConfiguration().region());
        assertEquals(Region.US_WEST_2, sesBackup.serviceClientConfiguration().region());
        assertEquals(ecrBackup.serviceClientConfiguration().region(), sesBackup.serviceClientConfiguration().region());
        assertEquals(Region.EU_WEST_1, unconfigured.serviceClientConfiguration().region());
        assertEquals(Region.AP_SOUTH_1, archive.serviceClientConfiguration().region());
        assertEquals("global-key", ((AwsCredentialsProvider) unconfigured.serviceClientConfiguration().credentialsProvider())
                .resolveCredentials().accessKeyId());
        assertEquals("global-key", ((AwsCredentialsProvider) archive.serviceClientConfiguration().credentialsProvider())
                .resolveCredentials().accessKeyId());
        assertEquals(URI.create("http://localhost:4566"),
                sesBackup.serviceClientConfiguration().endpointOverride().orElseThrow());
        assertEquals(Duration.ofSeconds(3),
                sesBackup.serviceClientConfiguration().overrideConfiguration().apiCallTimeout().orElseThrow());
        assertNotSame(ses, sesBackup);
        assertInstanceOf(AnonymousCredentialsProvider.class, sesBackup.serviceClientConfiguration().credentialsProvider());
    }

    @Test
    void integratesAsyncClient() {
        assertEquals(Region.EU_CENTRAL_1, async.serviceClientConfiguration().region());
        assertEquals("mail-key", ((AwsCredentialsProvider) async.serviceClientConfiguration().credentialsProvider())
                .resolveCredentials().accessKeyId());
    }

    @Test
    void acceptsClientNamesMatchingRootSettings() {
        assertEquals(Region.US_EAST_2, namedRegion.serviceClientConfiguration().region());
        assertEquals(Region.US_EAST_1, namedDevServices.serviceClientConfiguration().region());
        assertEquals(Region.EU_WEST_1, ecr.serviceClientConfiguration().region());
    }

    @Test
    void generatesFactoriesOnlyForRequestedClientTypes() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        String prefix = "io/quarkiverse/amazon/sdk/runtime/generated/software/amazon/awssdk/services/";
        assertNotNull(loader.getResource(prefix + "ecr/EcrClientBuilderFactory.class"));
        assertNull(loader.getResource(prefix + "ecr/EcrAsyncClientBuilderFactory.class"));
        assertNotNull(loader.getResource(prefix + "sesv2/SesV2ClientBuilderFactory.class"));
        assertNotNull(loader.getResource(prefix + "sesv2/SesV2AsyncClientBuilderFactory.class"));
    }
}
