package io.quarkiverse.amazon.common.test;

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

import io.quarkus.test.QuarkusExtensionTest;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.core.exception.SdkClientException;

public abstract class AbstractCredentialsProviderConfigTest {
    private static final String CUSTOM_PROVIDER_NAME = "test-creds-profile";

    @Inject
    @Named(CUSTOM_PROVIDER_NAME)
    AwsCredentialsProvider customProvider;

    public static QuarkusExtensionTest configure(QuarkusExtensionTest app, String prefix, String awsSuffix) {
        String root = prefix + awsSuffix;
        String profile = prefix + ".profile" + awsSuffix;
        String statik = prefix + ".static" + awsSuffix;
        String custom = prefix + ".custom" + awsSuffix;
        return app.overrideConfigKey("quarkus.devservices.enabled", "false")
                .overrideConfigKey(root + ".region", "us-east-2")
                .overrideConfigKey(root + ".credentials.type", "default")
                .overrideConfigKey(root + ".credentials.default-provider.async-credential-update-enabled", "true")
                .overrideConfigKey(root + ".credentials.default-provider.reuse-last-provider-enabled", "true")
                .overrideConfigKey(profile + ".region", "us-east-2")
                .overrideConfigKey(profile + ".credentials.type", "profile")
                .overrideConfigKey(profile + ".credentials.profile-provider.profile-name", "myprofile")
                .overrideConfigKey(statik + ".region", "us-east-2")
                .overrideConfigKey(statik + ".credentials.type", "static")
                .overrideConfigKey(statik + ".credentials.static-provider.access-key-id", "test-key")
                .overrideConfigKey(statik + ".credentials.static-provider.secret-access-key", "test-secret")
                .overrideConfigKey(custom + ".region", "us-east-2")
                .overrideConfigKey(custom + ".credentials.type", "custom")
                .overrideConfigKey(custom + ".credentials.custom-provider.name", CUSTOM_PROVIDER_NAME);
    }

    /** Default, profile, static and custom clients, in that order. */
    protected abstract AwsClient[] clients();

    protected abstract void callCustomClient();

    @Test
    public void resolvesConfiguredProvidersAndUsesTheCustomBeanToSignRequests() {
        AwsClient[] clients = clients();
        assertInstanceOf(DefaultCredentialsProvider.class, clients[0].serviceClientConfiguration().credentialsProvider());
        assertInstanceOf(ProfileCredentialsProvider.class, clients[1].serviceClientConfiguration().credentialsProvider());
        var statik = assertInstanceOf(StaticCredentialsProvider.class,
                clients[2].serviceClientConfiguration().credentialsProvider());
        assertEquals("test-key", statik.resolveCredentials().accessKeyId());
        assertSame(customProvider, clients[3].serviceClientConfiguration().credentialsProvider());
        SdkClientException error = assertThrows(SdkClientException.class, this::callCustomClient);
        assertTrue(error.getMessage().startsWith("Profile file contained no credentials for profile 'expected-not-exist'"));
    }

    @ApplicationScoped
    public static class CustomProvider {
        @Named(CUSTOM_PROVIDER_NAME)
        @Produces
        @ApplicationScoped
        public AwsCredentialsProvider provider() {
            return ProfileCredentialsProvider.create("expected-not-exist");
        }
    }
}
