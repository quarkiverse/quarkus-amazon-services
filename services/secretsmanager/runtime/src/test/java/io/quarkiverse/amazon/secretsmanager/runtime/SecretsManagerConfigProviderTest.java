package io.quarkiverse.amazon.secretsmanager.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkiverse.amazon.common.config.FetchFailure;
import io.quarkiverse.amazon.common.config.ResourceId;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

class SecretsManagerConfigProviderTest {
    @Test
    void deduplicatesExplicitReadsAndDoesNotDecodeBinary() {
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        byte[] bytes = { (byte) 0xff, (byte) 0xfe };
        when(client.getSecretValue(any(GetSecretValueRequest.class)))
                .thenReturn(
                        GetSecretValueResponse.builder().secretBinary(SdkBytes.fromByteArray(bytes)).versionId("v1").build());
        try (var provider = new SecretsManagerConfigProvider(client, List.of("secret", "secret"))) {
            var document = provider.fetchAll().updated().get(new ResourceId("secret"));
            assertArrayEquals(bytes, document.content());
            assertEquals("v1", document.version());
            verify(client, times(1)).getSecretValue(any(GetSecretValueRequest.class));
        }
    }

    @Test
    void failedExplicitReadRetainsSuccessfulDocumentsWithoutConfirmingDeletion() {
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(GetSecretValueRequest.class)))
                .thenReturn(GetSecretValueResponse.builder().secretString("value").build())
                .thenThrow(SdkClientException.create("network"));
        try (var provider = new SecretsManagerConfigProvider(client, List.of("a", "b"))) {
            var partial = provider.fetchAll();
            assertEquals(FetchFailure.OTHER, partial.failed().get(new ResourceId("b")));
            assertEquals(Set.of(new ResourceId("a")), partial.updated().keySet());
            assertTrue(partial.removed().isEmpty());
        }
    }

    @Test
    void emptySelectionDoesNotDiscoverOrReadSecrets() {
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        try (var provider = new SecretsManagerConfigProvider(client, List.of())) {
            assertTrue(provider.fetchAll().updated().isEmpty());
            verifyNoInteractions(client);
        }
    }
}
