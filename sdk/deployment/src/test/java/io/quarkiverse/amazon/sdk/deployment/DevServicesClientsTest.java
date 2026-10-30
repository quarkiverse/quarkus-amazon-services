package io.quarkiverse.amazon.sdk.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.net.URI;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.sesv2.SesV2AsyncClient;

/**
 * Verifies stub stack configuration reaches injected clients without overriding external endpoints or explicit regions and
 * credentials.
 * Historical counterpart: {@code io.quarkiverse.amazon.fake.deployment.devservices.DevServicesClientsTest}.
 */
class DevServicesClientsTest {
    @RegisterExtension
    static final QuarkusExtensionTest app = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(StubAwsStackProcessor.class)
                    .addAsResource(new StringAsset(StubAwsStackProcessor.class.getName()),
                            "META-INF/quarkus-build-steps.list"))
            // Exercise the same provider/config handoff without starting Docker.
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.aws.\"remote\".endpoint-override", "https://external.example")
            .overrideConfigKey("quarkus.aws.\"remote\".region", "us-west-2")
            .overrideConfigKey("quarkus.aws.\"custom\".region", "eu-central-1")
            .overrideConfigKey("quarkus.aws.\"custom\".credentials.type", "static")
            .overrideConfigKey("quarkus.aws.\"custom\".credentials.static-provider.access-key-id", "custom-key")
            .overrideConfigKey("quarkus.aws.\"custom\".credentials.static-provider.secret-access-key", "custom-secret");

    @Inject
    EcrClient defaultClient;

    @Inject
    @AmazonClient("local")
    EcrClient local;

    @Inject
    @AmazonClient("remote")
    EcrClient remote;

    @Inject
    @AmazonClient("custom")
    EcrClient custom;

    @Inject
    @AmazonClient("async.local")
    SesV2AsyncClient async;

    @ConfigProperty(name = "test.aws.requested-services")
    String services;

    @Test
    void configuresOnlyRequestedClientsWithoutAnExplicitEndpoint() {
        assertEquals("[ecr, sesv2]", services);
        assertEquals(URI.create("http://stub-aws:4566"),
                defaultClient.serviceClientConfiguration().endpointOverride().orElseThrow());
        assertEquals(URI.create("http://stub-aws:4566"), local.serviceClientConfiguration().endpointOverride().orElseThrow());
        assertEquals(Region.EU_WEST_3, local.serviceClientConfiguration().region());
        assertEquals("stub-key", ((AwsCredentialsProvider) local.serviceClientConfiguration().credentialsProvider())
                .resolveCredentials().accessKeyId());
        assertEquals(URI.create("http://stub-aws:4566"), async.serviceClientConfiguration().endpointOverride().orElseThrow());
        assertEquals(Region.EU_WEST_3, async.serviceClientConfiguration().region());
        assertEquals("stub-secret", ((AwsCredentialsProvider) async.serviceClientConfiguration().credentialsProvider())
                .resolveCredentials().secretAccessKey());
    }

    @Test
    void keepsExternalNamedClientOffTheStackAndItsCredentials() {
        assertEquals(URI.create("https://external.example"),
                remote.serviceClientConfiguration().endpointOverride().orElseThrow());
        assertEquals(Region.US_WEST_2, remote.serviceClientConfiguration().region());
        assertInstanceOf(DefaultCredentialsProvider.class, remote.serviceClientConfiguration().credentialsProvider());
    }

    @Test
    void keepsExplicitNamedRegionAndIdentityWhenUsingAnEmulator() {
        assertEquals(URI.create("http://stub-aws:4566"), custom.serviceClientConfiguration().endpointOverride().orElseThrow());
        assertEquals(Region.EU_CENTRAL_1, custom.serviceClientConfiguration().region());
        var credentials = ((AwsCredentialsProvider) custom.serviceClientConfiguration().credentialsProvider())
                .resolveCredentials();
        assertEquals("custom-key", credentials.accessKeyId());
        assertEquals("custom-secret", credentials.secretAccessKey());
    }
}
