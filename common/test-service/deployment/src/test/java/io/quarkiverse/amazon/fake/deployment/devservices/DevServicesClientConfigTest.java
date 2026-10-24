package io.quarkiverse.amazon.fake.deployment.devservices;

import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.NETTY;
import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.URL_CONNECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.fake.deployment.SdkHttpClients;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.fake.FakeAsyncClient;
import software.amazon.awssdk.services.fake.FakeClient;
import software.amazon.awssdk.services.fake.FakeServiceClientConfiguration;

/**
 * The clients are configured to reach the stack started by the dev services.
 */
public class DevServicesClientConfigTest {

    @Inject
    FakeClient sync;

    @Inject
    FakeAsyncClient async;

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
            .overrideConfigKey("quarkus.devservices.enabled", "false");

    @Test
    public void test() {
        assertEquals("[fake-service]", requestedServices);
        assertStackConfig(sync.serviceClientConfiguration());
        assertStackConfig(async.serviceClientConfiguration());
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
