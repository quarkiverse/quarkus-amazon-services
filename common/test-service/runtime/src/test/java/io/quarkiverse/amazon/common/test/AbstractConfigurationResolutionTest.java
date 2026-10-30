package io.quarkiverse.amazon.common.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkiverse.amazon.common.runtime.AmazonClientCommonRecorder;
import io.quarkiverse.amazon.common.runtime.AwsCredentialsProviderType;
import io.quarkiverse.amazon.common.runtime.HasAmazonClientRuntimeConfig;
import io.quarkiverse.amazon.common.runtime.RuntimeConfigurationError;

/** Shared inheritance and diagnostic edge cases for dedicated and generic client configuration. */
public abstract class AbstractConfigurationResolutionTest {
    protected abstract HasAmazonClientRuntimeConfig configuration(Map<String, String> properties);

    protected abstract String prefix();

    protected abstract String awsSuffix();

    @Test
    public void doesNotMergeCredentialGroupsAcrossLayers() {
        String root = prefix() + awsSuffix();
        String named = prefix() + ".local" + awsSuffix();
        var config = configuration(Map.of(root + ".credentials.type", "static",
                root + ".credentials.static-provider.access-key-id", "global-key",
                root + ".credentials.static-provider.secret-access-key", "global-secret",
                named + ".credentials.static-provider.access-key-id", "local-key"));
        var local = config.clients().get("local").aws().credentials().orElseThrow();
        assertEquals(AwsCredentialsProviderType.DEFAULT, local.type());
        assertEquals("local-key", local.staticProvider().accessKeyId().orElseThrow());
        assertTrue(local.staticProvider().secretAccessKey().isEmpty());
        var inherited = config.clients().get("unconfigured").aws().credentials()
                .or(() -> config.defaultsFor("unconfigured").aws().credentials()).orElseThrow();
        assertEquals(AwsCredentialsProviderType.STATIC, inherited.type());
    }

    @Test
    public void reportsTheGlobalSourceOfInheritedInvalidCredentials() {
        String source = prefix() + awsSuffix();
        var config = configuration(Map.of(source + ".credentials.type", "static",
                source + ".credentials.static-provider.access-key-id", "partial-key"));
        var error = assertThrows(RuntimeConfigurationError.class, () -> AmazonClientCommonRecorder.resolveCredentialsProvider(
                prefix().substring("quarkus.".length()), prefix().substring("quarkus.".length()) + ".\"archive\"",
                config.clients().get("archive").aws(), config.defaultsFor("archive").aws()));
        assertEquals(source + ".credentials.static-provider.access-key-id and " + source
                + ".credentials.static-provider.secret-access-key cannot be empty if STATIC credentials provider used.",
                error.getMessage());
    }

    @Test
    public void reportsTheGlobalSourceOfInheritedInvalidEndpoints() {
        var config = configuration(Map.of(prefix() + ".endpoint-override", "localhost"));
        var error = assertThrows(RuntimeConfigurationError.class, () -> AmazonClientCommonRecorder.resolveEndpointOverride(
                prefix().substring("quarkus.".length()), prefix().substring("quarkus.".length()) + ".\"archive\"",
                config.clients().get("archive").sdk(), config.defaultsFor("archive").sdk()));
        assertEquals(prefix() + ".endpoint-override (localhost) - scheme must be specified", error.getMessage());
    }

    @Test
    public void neverCombinesNamedAccessKeyWithGlobalSecret() {
        String root = prefix() + awsSuffix();
        String named = prefix() + ".\"partial\"" + awsSuffix();
        var config = configuration(Map.of(root + ".credentials.type", "static",
                root + ".credentials.static-provider.access-key-id", "global-key",
                root + ".credentials.static-provider.secret-access-key", "global-secret",
                named + ".credentials.type", "static", named + ".credentials.static-provider.access-key-id", "other-key"));
        var error = assertThrows(RuntimeConfigurationError.class, () -> AmazonClientCommonRecorder.resolveCredentialsProvider(
                prefix().substring("quarkus.".length()), prefix().substring("quarkus.".length()) + ".\"partial\"",
                config.clients().get("partial").aws(), config.defaultsFor("partial").aws()));
        assertEquals(named + ".credentials.static-provider.access-key-id and " + named
                + ".credentials.static-provider.secret-access-key cannot be empty if STATIC credentials provider used.",
                error.getMessage());
    }
}
