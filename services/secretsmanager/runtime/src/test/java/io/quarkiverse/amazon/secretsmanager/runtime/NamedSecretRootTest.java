package io.quarkiverse.amazon.secretsmanager.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import io.smallrye.config.SmallRyeConfigBuilder;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

class NamedSecretRootTest {
    @Test
    void defaultAutoEntryWithoutPrefixExposesDocumentKeysButRejectsAnUntargetedScalar() {
        var config = new SmallRyeConfigBuilder().withMapping(SecretsManagerConfigConfig.class)
                .withDefaultValue("quarkus.secretsmanager.config.entries.root.secret-id", "document")
                .build().getConfigMapping(SecretsManagerConfigConfig.class);
        var entries = SecretsManagerConfigSourceFactory.entries(config);
        var client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(
                GetSecretValueResponse.builder().secretString("{\"feature\":{\"enabled\":true}}").build());
        try (var source = new SecretsManagerConfigSource(client, entries, 270, true)) {
            assertEquals("true", source.getValue("feature.enabled"));
        }
        var scalarClient = mock(SecretsManagerClient.class);
        when(scalarClient.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(
                GetSecretValueResponse.builder().secretString("raw secret").build());
        var error = assertThrows(IllegalStateException.class,
                () -> new SecretsManagerConfigSource(scalarClient, entries, 270, true));
        assertEquals("Unable to load quarkus.secretsmanager.config [INVALID_CONTENT]", error.getMessage());
    }
}
