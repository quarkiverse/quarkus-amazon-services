package io.quarkiverse.amazon.fake.deployment;

import static io.quarkiverse.amazon.fake.deployment.FakeClientInspector.configuration;
import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.NETTY;
import static io.quarkiverse.amazon.fake.deployment.SdkHttpClients.URL_CONNECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.common.AmazonClient;
import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.services.fake.FakeAsyncClient;
import software.amazon.awssdk.services.fake.FakeClient;

/**
 * Each named client is configured independently, under its own {@code quarkus.fake-service.<name>.} prefix.
 */
public class CredentialsProviderConfigTest {

    private static final String CUSTOM_PROVIDER_NAME = "test-creds-profile";

    @Inject
    FakeClient defaultClient;

    @Inject
    @AmazonClient("profile")
    FakeClient profileClient;

    @Inject
    @AmazonClient("static")
    FakeAsyncClient staticClient;

    @Inject
    @AmazonClient("custom")
    FakeClient customClient;

    @Inject
    @Named(CUSTOM_PROVIDER_NAME)
    AwsCredentialsProvider customProvider;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setForcedDependencies(SdkHttpClients.of(URL_CONNECTION, NETTY))
            .withApplicationRoot((jar) -> jar.addClasses(SdkHttpClients.class, FakeClientInspector.class))
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.fake-service.aws.region", "us-east-2")
            .overrideConfigKey("quarkus.fake-service.aws.credentials.type", "default")
            .overrideConfigKey("quarkus.fake-service.aws.credentials.default-provider.async-credential-update-enabled", "true")
            .overrideConfigKey("quarkus.fake-service.aws.credentials.default-provider.reuse-last-provider-enabled", "true")
            .overrideConfigKey("quarkus.fake-service.profile.aws.region", "us-east-2")
            .overrideConfigKey("quarkus.fake-service.profile.aws.credentials.type", "profile")
            .overrideConfigKey("quarkus.fake-service.profile.aws.credentials.profile-provider.profile-name", "myprofile")
            .overrideConfigKey("quarkus.fake-service.static.aws.region", "us-east-2")
            .overrideConfigKey("quarkus.fake-service.static.aws.credentials.type", "static")
            .overrideConfigKey("quarkus.fake-service.static.aws.credentials.static-provider.access-key-id", "test-key")
            .overrideConfigKey("quarkus.fake-service.static.aws.credentials.static-provider.secret-access-key",
                    "test-secret")
            .overrideConfigKey("quarkus.fake-service.custom.aws.region", "us-east-2")
            .overrideConfigKey("quarkus.fake-service.custom.aws.credentials.type", "custom")
            .overrideConfigKey("quarkus.fake-service.custom.aws.credentials.custom-provider.name", CUSTOM_PROVIDER_NAME);

    @Test
    public void test() {
        assertInstanceOf(DefaultCredentialsProvider.class, credentialsProvider(defaultClient));
        assertInstanceOf(ProfileCredentialsProvider.class, credentialsProvider(profileClient));
        assertInstanceOf(StaticCredentialsProvider.class, credentialsProvider(staticClient));
        assertEquals("test-key",
                ((StaticCredentialsProvider) credentialsProvider(staticClient)).resolveCredentials().accessKeyId());
        assertSame(customProvider, credentialsProvider(customClient));

        // the custom provider is actually used to sign the requests
        SdkClientException expected = assertThrows(SdkClientException.class, () -> customClient.listThings());
        assertTrue(expected.getMessage()
                .startsWith("Profile file contained no credentials for profile 'expected-not-exist'"));
    }

    private static IdentityProvider<?> credentialsProvider(SdkClient client) {
        return configuration(client).option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER);
    }

    @Named(CUSTOM_PROVIDER_NAME)
    @Produces
    @ApplicationScoped
    public AwsCredentialsProvider provider() {
        return ProfileCredentialsProvider.create("expected-not-exist");
    }
}
