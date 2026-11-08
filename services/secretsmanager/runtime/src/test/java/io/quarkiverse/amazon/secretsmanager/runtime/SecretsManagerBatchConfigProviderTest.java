package io.quarkiverse.amazon.secretsmanager.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import io.quarkiverse.amazon.common.config.FetchFailure;
import io.quarkiverse.amazon.common.config.ResourceId;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.APIErrorType;
import software.amazon.awssdk.services.secretsmanager.model.BatchGetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.BatchGetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.SecretValueEntry;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

class SecretsManagerBatchConfigProviderTest {
    private static final String ARN = "arn:aws:secretsmanager:us-east-1:123456789012:secret:folder/db-AbCdEf";

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 20, 21, 41 })
    void deduplicatesThenChunksExplicitIdsWithoutListingOrIndividualReads(int count) {
        var client = mock(SecretsManagerClient.class);
        var ids = IntStream.range(0, count).mapToObj(i -> "secret-" + i).toList();
        when(client.batchGetSecretValue(any(BatchGetSecretValueRequest.class))).thenAnswer(call -> {
            BatchGetSecretValueRequest request = call.getArgument(0);
            return BatchGetSecretValueResponse.builder().secretValues(request.secretIdList().reversed().stream()
                    .map(id -> value(id, "value-" + id)).toList()).build();
        });
        var duplicated = new ArrayList<>(ids);
        duplicated.addAll(ids);
        try (var provider = new SecretsManagerConfigProvider(client, duplicated, true)) {
            var result = provider.fetchAll();
            assertEquals(count, result.updated().size());
            assertTrue(result.failed().isEmpty());
            assertTrue(result.removed().isEmpty());
            if (count == 0) {
                verifyNoInteractions(client);
            } else {
                var requests = ArgumentCaptor.forClass(BatchGetSecretValueRequest.class);
                verify(client, times((count + 19) / 20)).batchGetSecretValue(requests.capture());
                assertEquals(ids,
                        requests.getAllValues().stream().flatMap(request -> request.secretIdList().stream()).toList());
                for (var request : requests.getAllValues()) {
                    assertTrue(request.secretIdList().size() <= 20);
                    assertTrue(request.filters().isEmpty());
                    assertNull(request.nextToken());
                    assertNull(request.maxResults());
                }
                ids.forEach(id -> assertEquals("value-" + id, new String(result.updated().get(new ResourceId(id)).content(),
                        java.nio.charset.StandardCharsets.UTF_8)));
            }
        }
        verify(client).close();
        verifyNoMoreInteractions(client);
    }

    @Test
    void mapsNamesFullArnsAndPartialArnsToTheirConfiguredIdentities() {
        var client = mock(SecretsManagerClient.class);
        byte[] bytes = { (byte) 0xff, (byte) 0xfe };
        when(client.batchGetSecretValue(any(BatchGetSecretValueRequest.class))).thenReturn(
                BatchGetSecretValueResponse.builder().secretValues(
                        SecretValueEntry.builder().name("folder/db").arn(ARN).versionId("v1")
                                .secretBinary(SdkBytes.fromByteArray(bytes)).build())
                        .build());
        var ids = List.of("folder/db", ARN, ARN.substring(0, ARN.length() - 7));
        try (var provider = new SecretsManagerConfigProvider(client, ids, true)) {
            var result = provider.fetchAll();
            assertTrue(result.failed().isEmpty());
            assertEquals(3, result.updated().size());
            for (String id : ids) {
                var document = result.updated().get(new ResourceId(id));
                assertArrayEquals(bytes, document.content());
                assertEquals("v1", document.version());
            }
        }
    }

    @Test
    void fullArnWinsOverAnotherSecretsPartialArn() {
        var client = mock(SecretsManagerClient.class);
        when(client.batchGetSecretValue(any(BatchGetSecretValueRequest.class))).thenReturn(
                BatchGetSecretValueResponse.builder().secretValues(
                        SecretValueEntry.builder().name("folder/db").arn(ARN).secretString("first").build(),
                        SecretValueEntry.builder().name("folder/db-AbCdEf").arn(ARN + "-GhIjKl")
                                .secretString("second").build())
                        .build());
        try (var provider = new SecretsManagerConfigProvider(client, List.of(ARN, "folder/db-AbCdEf"), true)) {
            var result = provider.fetchAll();
            assertTrue(result.failed().isEmpty());
            assertEquals("first", new String(result.updated().get(new ResourceId(ARN)).content(),
                    java.nio.charset.StandardCharsets.UTF_8));
            assertEquals("second", new String(result.updated().get(new ResourceId("folder/db-AbCdEf")).content(),
                    java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    @ParameterizedTest
    @CsvSource({ "ResourceNotFoundException, MISSING", "AccessDeniedException, ACCESS_DENIED",
            "KMSAccessDeniedException, ACCESS_DENIED", "InternalServiceError, TRANSIENT", "ThrottlingException, TRANSIENT",
            "DecryptionFailure, OTHER", "InvalidRequestException, OTHER", ", OTHER" })
    void accountsForPerSecretErrorsWithoutLeakingTheirMessages(String code, FetchFailure expected) {
        var client = mock(SecretsManagerClient.class);
        when(client.batchGetSecretValue(any(BatchGetSecretValueRequest.class))).thenReturn(
                BatchGetSecretValueResponse.builder().secretValues(value("good", "value"))
                        .errors(APIErrorType.builder().secretId("bad").errorCode(code).message("unsafe secret content").build())
                        .build());
        try (var provider = new SecretsManagerConfigProvider(client, List.of("good", "bad"), true)) {
            var result = provider.fetchAll();
            assertEquals(Set.of(new ResourceId("good")), result.updated().keySet());
            assertEquals(expected, result.failed().get(new ResourceId("bad")));
            assertEquals(1, result.failed().size());
            assertTrue(result.removed().isEmpty());
        }
    }

    @Test
    void batchFailureDoesNotFallBackAndOtherChunksStillRun() {
        var client = mock(SecretsManagerClient.class);
        var ids = IntStream.range(0, 21).mapToObj(i -> "secret-" + i).toList();
        when(client.batchGetSecretValue(any(BatchGetSecretValueRequest.class)))
                .thenThrow(SecretsManagerException.builder().statusCode(403).message("unsafe IAM details").build())
                .thenReturn(BatchGetSecretValueResponse.builder().secretValues(value("secret-20", "value")).build());
        try (var provider = new SecretsManagerConfigProvider(client, ids, true)) {
            var result = provider.fetchAll();
            assertEquals(20, result.failed().size());
            assertTrue(result.failed().values().stream().allMatch(failure -> failure == FetchFailure.ACCESS_DENIED));
            assertEquals(Set.of(new ResourceId("secret-20")), result.updated().keySet());
            verify(client, times(2)).batchGetSecretValue(any(BatchGetSecretValueRequest.class));
            verify(client, never()).getSecretValue(any(GetSecretValueRequest.class));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "omitted", "duplicate", "duplicate-error", "value-and-error", "unknown-value", "unknown-error",
            "next-token",
            "missing-payload", "both-payloads", "ambiguous-partial-arn", "shadowed-unrequested-arn" })
    void malformedResponsesFailClosed(String scenario) {
        var client = mock(SecretsManagerClient.class);
        var response = BatchGetSecretValueResponse.builder();
        String requested = "secret";
        switch (scenario) {
            case "duplicate" -> response.secretValues(value("secret", "a"), value("secret", "b"));
            case "duplicate-error" -> response.errors(APIErrorType.builder().secretId("secret").build(),
                    APIErrorType.builder().secretId("secret").build());
            case "value-and-error" -> response.secretValues(value("secret", "a"))
                    .errors(APIErrorType.builder().secretId("secret").errorCode("AccessDeniedException").build());
            case "unknown-value" -> response.secretValues(value("secret", "a"), value("unrequested", "unsafe"));
            case "unknown-error" -> response.secretValues(value("secret", "a"))
                    .errors(APIErrorType.builder().secretId("unrequested").build());
            case "next-token" -> response.secretValues(value("secret", "a")).nextToken("unsafe token");
            case "missing-payload" -> response.secretValues(SecretValueEntry.builder().name("secret").build());
            case "both-payloads" -> response.secretValues(value("secret", "a").toBuilder()
                    .secretBinary(SdkBytes.fromUtf8String("b")).build());
            case "ambiguous-partial-arn" -> {
                requested = ARN.substring(0, ARN.length() - 7);
                response.secretValues(value("folder/db", "a").toBuilder().arn(ARN).build(),
                        value("folder/db", "b").toBuilder().arn(requested + "-GhIjKl").build());
            }
            case "shadowed-unrequested-arn" -> {
                requested = ARN;
                response.secretValues(value("folder/db", "a").toBuilder().arn(ARN).build(),
                        value("folder/db-AbCdEf", "unrequested").toBuilder().arn(ARN + "-GhIjKl").build());
            }
            default -> {
            }
        }
        when(client.batchGetSecretValue(any(BatchGetSecretValueRequest.class))).thenReturn(response.build());
        try (var provider = new SecretsManagerConfigProvider(client, List.of(requested), true)) {
            var result = provider.fetchAll();
            assertTrue(result.updated().isEmpty());
            assertEquals(1, result.failed().size());
            assertEquals(scenario.endsWith("payload") || scenario.equals("both-payloads") ? FetchFailure.INVALID_CONTENT
                    : FetchFailure.PARTIAL_RESPONSE, result.failed().get(new ResourceId(requested)));
        }
    }

    private static SecretValueEntry value(String name, String content) {
        return SecretValueEntry.builder().name(name).secretString(content).build();
    }
}
