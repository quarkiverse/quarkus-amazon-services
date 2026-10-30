package io.quarkiverse.amazon.fake.deployment.devservices;

import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.NETTY;
import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.URL_CONNECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.net.URI;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkiverse.amazon.fake.deployment.SdkHttpClients;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.fake.FakeAsyncClient;
import software.amazon.awssdk.services.fake.FakeClient;
import software.amazon.awssdk.services.fake.FakeServiceClientConfiguration;

/**
 * Covers the same client defaults and explicit settings as the generic DevServicesClientsTest, using the historical provider.
 */
public class DevServicesClientsTest {

    @Inject
    FakeClient sync;

    @Inject
    FakeAsyncClient async;

    @Inject
    @AmazonClient("local")
    FakeClient local;

    @Inject
    @AmazonClient("custom")
    FakeClient custom;

    @Inject
    @AmazonClient("remote")
    FakeClient remote;

    @Inject
    @AmazonClient("inherited")
    FakeClient inherited;

    @ConfigProperty(name = StubAwsStackProcessor.REQUESTED_SERVICES)
    String requestedServices;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setForcedDependencies(SdkHttpClients.of(URL_CONNECTION, NETTY))
            .withApplicationRoot((jar) -> jar
                    .addClass(SdkHttpClients.class)
                    .addClass(StubAwsStackProcessor.class)
                    .addAsResource(new StringAsset(StubAwsStackProcessor.class.getName()),
                            "META-INF/quarkus-build-steps.list"))
            // only the stub processor plays with the stack
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.fake-service.\"remote\".endpoint-override", "https://external.example")
            .overrideConfigKey("quarkus.fake-service.\"remote\".aws.region", "us-west-2")
            .overrideConfigKey("quarkus.fake-service.\"remote\".aws.credentials.type", "default")
            .overrideConfigKey("quarkus.fake-service.\"custom\".aws.region", "eu-central-1")
            .overrideConfigKey("quarkus.fake-service.\"custom\".aws.credentials.type", "static")
            .overrideConfigKey("quarkus.fake-service.\"custom\".aws.credentials.static-provider.access-key-id", "custom-key")
            .overrideConfigKey("quarkus.fake-service.\"custom\".aws.credentials.static-provider.secret-access-key",
                    "custom-secret")
            .overrideConfigKey("quarkus.fake-service.\"inherited\".endpoint-override", "https://external.example");

    @Test
    public void configuresOnlyRequestedClientsWithoutAnExplicitEndpoint() {
        assertEquals("[fake-service]", requestedServices);
        assertStackConfig(sync.serviceClientConfiguration());
        assertStackConfig(async.serviceClientConfiguration());
        assertStackConfig(local.serviceClientConfiguration());
    }

    @Test
    public void keepsExternalNamedClientOffTheStackAndItsCredentials() {
        assertEquals(URI.create("https://external.example"),
                remote.serviceClientConfiguration().endpointOverride().orElseThrow());
        assertEquals(Region.US_WEST_2, remote.serviceClientConfiguration().region());
        assertInstanceOf(DefaultCredentialsProvider.class, remote.serviceClientConfiguration().credentialsProvider());
    }

    @Test
    public void keepsExplicitNamedRegionAndIdentityWhenUsingAnEmulator() {
        assertEquals(StubAwsStackProcessor.STUB_CONTAINER.getEndpoint(),
                custom.serviceClientConfiguration().endpointOverride().orElseThrow());
        assertEquals(Region.EU_CENTRAL_1, custom.serviceClientConfiguration().region());
        var credentials = ((AwsCredentialsProvider) custom.serviceClientConfiguration().credentialsProvider())
                .resolveCredentials();
        assertEquals("custom-key", credentials.accessKeyId());
        assertEquals("custom-secret", credentials.secretAccessKey());
    }

    @Test
    public void externalNamedClientInheritsStackIdentityUnlessExplicitlyConfigured() {
        assertEquals(URI.create("https://external.example"),
                inherited.serviceClientConfiguration().endpointOverride().orElseThrow());
        assertEquals(Region.of(StubAwsStackProcessor.STUB_CONTAINER.getRegion()),
                inherited.serviceClientConfiguration().region());
        var credentials = ((AwsCredentialsProvider) inherited.serviceClientConfiguration().credentialsProvider())
                .resolveCredentials();
        assertEquals(StubAwsStackProcessor.STUB_CONTAINER.getAccessKey(), credentials.accessKeyId());
        assertEquals(StubAwsStackProcessor.STUB_CONTAINER.getSecretKey(), credentials.secretAccessKey());
    }

    private static void assertStackConfig(FakeServiceClientConfiguration configuration) {
        assertEquals(StubAwsStackProcessor.STUB_CONTAINER.getEndpoint(), configuration.endpointOverride().orElseThrow());
        assertEquals(Region.of(StubAwsStackProcessor.STUB_CONTAINER.getRegion()), configuration.region());
        var credentials = ((AwsCredentialsProvider) configuration.credentialsProvider()).resolveCredentials();
        assertEquals(StaticCredentialsProvider.class, configuration.credentialsProvider().getClass());
        assertEquals(StubAwsStackProcessor.STUB_CONTAINER.getAccessKey(), credentials.accessKeyId());
        assertEquals(StubAwsStackProcessor.STUB_CONTAINER.getSecretKey(), credentials.secretAccessKey());
    }
}
