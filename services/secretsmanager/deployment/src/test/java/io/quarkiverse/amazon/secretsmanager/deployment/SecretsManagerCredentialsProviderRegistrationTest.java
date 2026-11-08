package io.quarkiverse.amazon.secretsmanager.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.secretsmanager.runtime.SecretsManagerCredentialsProvider;
import io.quarkus.arc.Arc;
import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.credentials.runtime.CredentialsProviderFinder;
import io.quarkus.test.QuarkusExtensionTest;

class SecretsManagerCredentialsProviderRegistrationTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(ExistingCredentialsProvider.class))
            .overrideConfigKey("quarkus.devservices.enabled", "false");

    @Test
    void unusedProviderDoesNotRequireAnAwsRegionOrCredentialsAtStartup() {
        var beans = Arc.container().beanManager().getBeans("secretsmanager-credentials-provider");
        assertEquals(1, beans.size());
        assertEquals("secretsmanager-credentials-provider", beans.iterator().next().getName());
    }

    @Test
    void existingDefaultProviderStillResolvesAndSecretsManagerNeedsExplicitSelection() {
        var provider = CredentialsProviderFinder.find(null);
        assertEquals(Map.of(CredentialsProvider.USER_PROPERTY_NAME, "existing-user",
                CredentialsProvider.PASSWORD_PROPERTY_NAME, "existing-password"), provider.getCredentials("db"));
        assertTrue(CredentialsProviderFinder
                .find("secretsmanager-credentials-provider") instanceof SecretsManagerCredentialsProvider);
        var defaults = Arc.container().beanManager().getBeans(CredentialsProvider.class);
        assertEquals(1, defaults.size());
        assertEquals("existing-provider", defaults.iterator().next().getName());
    }

    @ApplicationScoped
    @Named("existing-provider")
    public static class ExistingCredentialsProvider implements CredentialsProvider {
        @Override
        public Map<String, String> getCredentials(String name) {
            return Map.of(USER_PROPERTY_NAME, "existing-user", PASSWORD_PROPERTY_NAME, "existing-password");
        }
    }
}
