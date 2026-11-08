package io.quarkiverse.amazon.ssm.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.amazon.ssm.runtime.SsmCredentialsProvider;
import io.quarkus.arc.Arc;
import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.credentials.runtime.CredentialsProviderFinder;
import io.quarkus.test.QuarkusExtensionTest;

class SsmCredentialsProviderRegistrationTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(ExistingCredentialsProvider.class))
            .overrideConfigKey("quarkus.devservices.enabled", "false");

    @Test
    void unusedProviderNeedsNoRegionAndDoesNotReplaceAnExistingDefault() {
        var beans = Arc.container().beanManager().getBeans("ssm-credentials-provider");
        assertEquals(1, beans.size());
        assertEquals("ssm-credentials-provider", beans.iterator().next().getName());
        assertEquals(Map.of("password", "existing"), CredentialsProviderFinder.find(null).getCredentials("db"));
        assertTrue(CredentialsProviderFinder.find("ssm-credentials-provider") instanceof SsmCredentialsProvider);
        assertEquals(1, Arc.container().beanManager().getBeans(CredentialsProvider.class).size());
    }

    @ApplicationScoped
    @Named("existing-provider")
    public static class ExistingCredentialsProvider implements CredentialsProvider {
        @Override
        public Map<String, String> getCredentials(String name) {
            return Map.of(PASSWORD_PROPERTY_NAME, "existing");
        }
    }
}
