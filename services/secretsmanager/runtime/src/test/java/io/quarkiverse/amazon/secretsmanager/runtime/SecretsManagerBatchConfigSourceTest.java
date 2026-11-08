package io.quarkiverse.amazon.secretsmanager.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.amazon.common.config.ConfigSourceEntry;
import io.quarkiverse.amazon.common.config.ResourceId;
import io.quarkiverse.amazon.common.runtime.ConfigSourceFormat;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.APIErrorType;
import software.amazon.awssdk.services.secretsmanager.model.BatchGetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.BatchGetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretValueEntry;

class SecretsManagerBatchConfigSourceTest {
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void partialBatchPublishesNothingAndAppliesStartupPolicy(boolean required) {
        var client = mock(SecretsManagerClient.class);
        when(client.batchGetSecretValue(any(BatchGetSecretValueRequest.class))).thenReturn(
                BatchGetSecretValueResponse.builder().secretValues(value("a", "value"))
                        .errors(APIErrorType.builder().secretId("b").errorCode("AccessDeniedException").message("unsafe")
                                .build())
                        .build());
        if (required) {
            var failure = assertThrows(IllegalStateException.class, () -> new SecretsManagerConfigSource(client,
                    List.of(entry("a", "a", ConfigSourceFormat.RAW), entry("b", "b", ConfigSourceFormat.RAW)), 270, true,
                    true));
            assertEquals("Unable to load quarkus.secretsmanager.config [ACCESS_DENIED]", failure.getMessage());
            assertNull(failure.getCause());
        } else {
            var source = new SecretsManagerConfigSource(client,
                    List.of(entry("a", "a", ConfigSourceFormat.RAW), entry("b", "b", ConfigSourceFormat.RAW)), 270, false,
                    true);
            assertTrue(source.getPropertyNames().isEmpty());
        }
        verify(client).close();
    }

    @ParameterizedTest
    @ValueSource(strings = { "json", "utf8", "collision" })
    void decodingOrCollisionErrorsRejectTheWholeSnapshot(String scenario) {
        var client = mock(SecretsManagerClient.class);
        var bad = switch (scenario) {
            case "json" -> value("b", "{unsafe");
            case "utf8" -> SecretValueEntry.builder().name("b")
                    .secretBinary(SdkBytes.fromByteArray(new byte[] { (byte) 0xff })).build();
            default -> value("b", "{\"password\":\"value\"}");
        };
        when(client.batchGetSecretValue(any(BatchGetSecretValueRequest.class))).thenReturn(
                BatchGetSecretValueResponse.builder().secretValues(value("a", "{\"password\":\"value\"}"), bad).build());
        var failure = assertThrows(IllegalStateException.class, () -> new SecretsManagerConfigSource(client,
                List.of(entry("a", "target", ConfigSourceFormat.JSON), entry("b", "target", ConfigSourceFormat.JSON)),
                270, true, true));
        assertEquals("Unable to load quarkus.secretsmanager.config [INVALID_CONTENT]", failure.getMessage());
        assertNull(failure.getCause());
        verify(client).close();
    }

    @Test
    void successfulBinaryPayloadIsDecodedAsUtf8() {
        var client = mock(SecretsManagerClient.class);
        when(client.batchGetSecretValue(any(BatchGetSecretValueRequest.class))).thenReturn(
                BatchGetSecretValueResponse.builder().secretValues(SecretValueEntry.builder().name("a")
                        .secretBinary(SdkBytes.fromUtf8String("été")).build()).build());
        var source = new SecretsManagerConfigSource(client, List.of(entry("a", "text", ConfigSourceFormat.RAW)),
                270, true, true);
        assertEquals("été", source.getValue("text"));
        verify(client).close();
    }

    @Test
    void failureInLastChunkDoesNotPublishEarlierSuccessfulChunks() {
        var client = mock(SecretsManagerClient.class);
        var entries = java.util.stream.IntStream.range(0, 21)
                .mapToObj(i -> entry("secret-" + i, "property-" + i, ConfigSourceFormat.RAW)).toList();
        when(client.batchGetSecretValue(any(BatchGetSecretValueRequest.class))).thenAnswer(call -> {
            BatchGetSecretValueRequest request = call.getArgument(0);
            if (request.secretIdList().contains("secret-20")) {
                return BatchGetSecretValueResponse.builder().build();
            }
            return BatchGetSecretValueResponse.builder().secretValues(request.secretIdList().stream()
                    .map(id -> value(id, "value")).toList()).build();
        });
        var source = new SecretsManagerConfigSource(client, entries, 270, false, true);
        assertTrue(source.getPropertyNames().isEmpty());
        verify(client, org.mockito.Mockito.times(2)).batchGetSecretValue(any(BatchGetSecretValueRequest.class));
        verify(client).close();
    }

    @Test
    void exactArnCannotHideAnUnrequestedPartialArnMatch() {
        String arn = "arn:aws:secretsmanager:us-east-1:123456789012:secret:db-AbCdEf";
        var client = mock(SecretsManagerClient.class);
        when(client.batchGetSecretValue(any(BatchGetSecretValueRequest.class))).thenReturn(
                BatchGetSecretValueResponse.builder().secretValues(
                        value("db", "selected").toBuilder().arn(arn).build(),
                        value("db-AbCdEf", "unrequested").toBuilder().arn(arn + "-GhIjKl").build()).build());
        var failure = assertThrows(IllegalStateException.class, () -> new SecretsManagerConfigSource(client,
                List.of(entry(arn, "target", ConfigSourceFormat.RAW)), 270, true, true));
        assertEquals("Unable to load quarkus.secretsmanager.config [PARTIAL_RESPONSE]", failure.getMessage());
        assertNull(failure.getCause());
        verify(client).close();
    }

    private static ConfigSourceEntry entry(String secret, String prefix, ConfigSourceFormat format) {
        return new ConfigSourceEntry(secret, new ResourceId(secret), prefix, format, false);
    }

    private static SecretValueEntry value(String name, String content) {
        return SecretValueEntry.builder().name(name).secretString(content).build();
    }
}
