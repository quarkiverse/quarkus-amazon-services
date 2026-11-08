package io.quarkiverse.amazon.secretsmanager.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import io.quarkus.credentials.CredentialsProvider;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.APIErrorType;
import software.amazon.awssdk.services.secretsmanager.model.BatchGetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.BatchGetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretValueEntry;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

class SecretsManagerCredentialsProviderTest {
    @Test
    void everyReadUsesAwscurrentAndReturnsOnlyAnImmutableCredentialPair() {
        var provider = provider();
        when(provider.client.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(
                response("{\"username\":\"first\",\"password\":\"initial\",\"expires-at\":\"tomorrow\"}"),
                response("{\"username\":\"second\",\"password\":\" rotated\\n${literal} \"}"));
        var first = provider.getCredentials("db");
        assertEquals(
                Map.of(CredentialsProvider.USER_PROPERTY_NAME, "first", CredentialsProvider.PASSWORD_PROPERTY_NAME, "initial"),
                first);
        assertThrows(UnsupportedOperationException.class, () -> first.put("password", "other"));
        var second = provider.getCredentials("db");
        assertEquals("second", second.get(CredentialsProvider.USER_PROPERTY_NAME));
        assertEquals(" rotated\n${literal} ", second.get(CredentialsProvider.PASSWORD_PROPERTY_NAME));
        assertFalse(second.containsKey(CredentialsProvider.EXPIRATION_TIMESTAMP_PROPERTY_NAME));
        var requests = ArgumentCaptor.forClass(GetSecretValueRequest.class);
        verify(provider.client, times(2)).getSecretValue(requests.capture());
        for (var request : requests.getAllValues()) {
            assertEquals("arn:aws:secretsmanager:us-east-1:123456789012:secret:database-AbCdEf", request.secretId());
            assertEquals("AWSCURRENT", request.versionStage());
            assertTrue(request.overrideConfiguration().isEmpty());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "not json secret", "{}", "[]", "null", "{broken unsafe", "{} trailing",
            "{\"username\":\"user\"}", "{\"username\":null,\"password\":\"unsafe\"}",
            "{\"username\":12,\"password\":\"unsafe\"}", "{\"username\":\"user\",\"password\":[]}",
            "{\"username\":\"\",\"password\":\"unsafe\"}", "{\"username\":\"user\",\"password\":\"\"}",
            "{\"username\":\"user\",\"password\":\"\\ud800\"}" })
    void malformedOrIncompleteCredentialsFailWithoutContentOrCause(String content) {
        var provider = provider();
        when(provider.client.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(response(content));
        assertSafeFailure(provider, "INVALID_CONTENT");
    }

    @Test
    void binarySecretsUseStrictUtf8AndAnEmptyResponseFails() {
        var provider = provider();
        when(provider.client.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(
                GetSecretValueResponse.builder().secretBinary(SdkBytes.fromByteArray(
                        "{\"username\":\"été\",\"password\":\"value\"}".getBytes(StandardCharsets.UTF_8))).build(),
                GetSecretValueResponse.builder().secretBinary(SdkBytes.fromByteArray(new byte[] { (byte) 0xff })).build(),
                GetSecretValueResponse.builder().build());
        assertEquals("été", provider.getCredentials("db").get(CredentialsProvider.USER_PROPERTY_NAME));
        assertSafeFailure(provider, "INVALID_CONTENT");
        assertSafeFailure(provider, "INVALID_CONTENT");
    }

    @Test
    void failuresNeverServeOldCredentialsAndTheNextReadCanRecover() {
        var provider = provider();
        when(provider.client.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(
                response("{\"username\":\"user\",\"password\":\"old\"}"))
                .thenThrow(SecretsManagerException.builder().statusCode(403).message("unsafe IAM message").build())
                .thenThrow(SecretsManagerException.builder().statusCode(404).message("unsafe missing message").build())
                .thenThrow(SdkClientException.builder().message("unsafe network message").cause(new IOException()).build())
                .thenReturn(response("{\"username\":\"user\",\"password\":\"new\"}"));
        assertEquals("old", provider.getCredentials("db").get(CredentialsProvider.PASSWORD_PROPERTY_NAME));
        assertSafeFailure(provider, "ACCESS_DENIED");
        assertSafeFailure(provider, "MISSING");
        assertSafeFailure(provider, "TRANSIENT");
        assertEquals("new", provider.getCredentials("db").get(CredentialsProvider.PASSWORD_PROPERTY_NAME));
    }

    @Test
    void unknownNamesDoNotReadAws() {
        var provider = provider();
        for (String name : new String[] { null, "", "unsafe unknown name" }) {
            var failure = assertThrows(IllegalArgumentException.class, () -> provider.getCredentials(name));
            assertEquals("Unknown Secrets Manager credentials provider.", failure.getMessage());
            assertNull(failure.getCause());
        }
        verifyNoInteractions(provider.client);
    }

    @Test
    void namedSetsSelectTheirOwnSecretsAndBlankIdsNeverReachAws() {
        var provider = provider();
        provider.config = new SmallRyeConfigBuilder().withMapping(SecretsManagerCredentialsProviderConfig.class)
                .withDefaultValue("quarkus.secretsmanager.credentials-provider.db.secret-id", "database")
                .withDefaultValue("quarkus.secretsmanager.credentials-provider.other.secret-id", "other-database")
                .withDefaultValue("quarkus.secretsmanager.credentials-provider.blank.secret-id", " ")
                .build().getConfigMapping(SecretsManagerCredentialsProviderConfig.class);
        when(provider.client.getSecretValue(any(GetSecretValueRequest.class))).thenAnswer(invocation -> {
            GetSecretValueRequest request = invocation.getArgument(0);
            return response("{\"username\":\"user\",\"password\":\"" + request.secretId() + "\"}");
        });
        var failure = assertThrows(IllegalStateException.class, () -> provider.getCredentials("blank"));
        assertEquals("Unable to load Secrets Manager credentials [INVALID_CONTENT]", failure.getMessage());
        verifyNoInteractions(provider.client);
        assertEquals("database", provider.getCredentials("db").get(CredentialsProvider.PASSWORD_PROPERTY_NAME));
        assertEquals("other-database", provider.getCredentials("other").get(CredentialsProvider.PASSWORD_PROPERTY_NAME));
    }

    @Test
    void concurrentReadsNeverMixUsersAndPasswords() throws Exception {
        var provider = provider();
        var calls = new AtomicInteger();
        when(provider.client.getSecretValue(any(GetSecretValueRequest.class))).thenAnswer(invocation -> {
            int version = calls.incrementAndGet();
            return response("{\"username\":\"u" + version + "\",\"password\":\"p" + version + "\"}");
        });
        var executor = Executors.newFixedThreadPool(8);
        try {
            var results = new ArrayList<java.util.concurrent.Future<Map<String, String>>>();
            for (int i = 0; i < 32; i++) {
                results.add(executor.submit(() -> provider.getCredentials("db")));
            }
            for (var result : results) {
                var credentials = result.get(5, TimeUnit.SECONDS);
                assertEquals("p" + credentials.get(CredentialsProvider.USER_PROPERTY_NAME).substring(1),
                        credentials.get(CredentialsProvider.PASSWORD_PROPERTY_NAME));
            }
            assertEquals(32, calls.get());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "json", "properties", "raw" })
    void documentsSupportExplicitFormatsAndAnExternalUsername(String format) {
        var provider = provider(Map.of("secret-id", "db", "format", format));
        String text = switch (format) {
            case "json" -> "{\"password\":\"secret\"}";
            case "properties" -> "password=secret";
            default -> "secret";
        };
        when(provider.client.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(response(text));
        assertEquals(Map.of("password", "secret"), provider.getCredentials("db"));
    }

    @Test
    void documentKeysAreConfigurableAndUsernameCanBeExplicitlyDisabled() {
        var provider = provider(Map.of("secret-id", "db", "username-key", "login", "password-key", "pwd"));
        when(provider.client.getSecretValue(any(GetSecretValueRequest.class)))
                .thenReturn(response("{\"login\":\"app\",\"pwd\":\"secret\"}"));
        assertEquals(Map.of("user", "app", "password", "secret"), provider.getCredentials("db"));
        var passwordOnly = provider(Map.of("secret-id", "db", "username-key", ""));
        when(passwordOnly.client.getSecretValue(any(GetSecretValueRequest.class)))
                .thenReturn(response("{\"username\":\"ignored\",\"password\":\"secret\"}"));
        assertEquals(Map.of("password", "secret"), passwordOnly.getCredentials("db"));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void explicitRawKeysAreReadCompletelyWithOptionalBatching(boolean batch) {
        var provider = provider(Map.of("secrets.user", "login", "secrets.password", "pwd",
                "batch-retrieval", Boolean.toString(batch)));
        when(provider.client.getSecretValue(any(GetSecretValueRequest.class))).thenAnswer(call -> {
            GetSecretValueRequest request = call.getArgument(0);
            return response(request.secretId().equals("login") ? "app" : "secret");
        });
        when(provider.client.batchGetSecretValue(any(BatchGetSecretValueRequest.class)))
                .thenReturn(BatchGetSecretValueResponse.builder().secretValues(
                        SecretValueEntry.builder().name("pwd").secretString("secret").build(),
                        SecretValueEntry.builder().name("login").secretString("app").build()).build());
        assertEquals(Map.of("user", "app", "password", "secret"), provider.getCredentials("db"));
        assertEquals(Map.of("user", "app", "password", "secret"), provider.getCredentials("db"));
        verify(provider.client, times(batch ? 2 : 0)).batchGetSecretValue(any(BatchGetSecretValueRequest.class));
        verify(provider.client, times(batch ? 0 : 4)).getSecretValue(any(GetSecretValueRequest.class));
        verify(provider.client, never()).close();
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void repeatedIdentitiesShareOneReadAndPartialFailuresNeverReturnCredentials(boolean batch) {
        var provider = provider(Map.of("secrets.user", "shared", "secrets.password", "shared",
                "batch-retrieval", Boolean.toString(batch)));
        when(provider.client.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(response("same"));
        when(provider.client.batchGetSecretValue(any(BatchGetSecretValueRequest.class)))
                .thenReturn(BatchGetSecretValueResponse.builder().secretValues(
                        SecretValueEntry.builder().name("shared").secretString("same").build()).build());
        assertEquals(Map.of("user", "same", "password", "same"), provider.getCredentials("db"));
        verify(provider.client, times(batch ? 0 : 1)).getSecretValue(any(GetSecretValueRequest.class));
        verify(provider.client, times(batch ? 1 : 0)).batchGetSecretValue(any(BatchGetSecretValueRequest.class));

        var partial = provider(Map.of("secrets.user", "login", "secrets.password", "pwd",
                "batch-retrieval", Boolean.toString(batch)));
        when(partial.client.getSecretValue(any(GetSecretValueRequest.class))).thenAnswer(call -> {
            GetSecretValueRequest request = call.getArgument(0);
            if (request.secretId().equals("pwd")) {
                throw SecretsManagerException.builder().statusCode(403).message("unsafe secret").build();
            }
            return response("app");
        });
        when(partial.client.batchGetSecretValue(any(BatchGetSecretValueRequest.class)))
                .thenReturn(BatchGetSecretValueResponse.builder().secretValues(
                        SecretValueEntry.builder().name("login").secretString("app").build())
                        .errors(APIErrorType.builder().secretId("pwd").errorCode("AccessDeniedException")
                                .message("unsafe secret").build())
                        .build());
        assertSafeFailure(partial, "ACCESS_DENIED");
        if (batch) {
            verify(partial.client, never()).getSecretValue(any(GetSecretValueRequest.class));
        }
    }

    @Test
    void singleDocumentCanUseBatchWithoutIndividualFallback() {
        var provider = provider(Map.of("secret-id", "db", "batch-retrieval", "true"));
        when(provider.client.batchGetSecretValue(any(BatchGetSecretValueRequest.class)))
                .thenReturn(BatchGetSecretValueResponse.builder().secretValues(SecretValueEntry.builder().name("db")
                        .secretString("{\"password\":\"secret\"}").build()).build())
                .thenThrow(SecretsManagerException.builder().statusCode(403).message("unsafe").build());
        assertEquals(Map.of("password", "secret"), provider.getCredentials("db"));
        assertSafeFailure(provider, "ACCESS_DENIED");
        verify(provider.client, never()).getSecretValue(any(GetSecretValueRequest.class));
    }

    @Test
    void invalidSelectionsFailBeforeAnyAwsRead() {
        for (var properties : java.util.List.of(
                Map.of("secret-id", "db", "secrets.password", "other"),
                Map.of("secrets.user", "login"),
                Map.of("secrets.password", "pwd", "secrets.expires-at", "expiry"),
                Map.of("secret-id", "db", "format", "auto"))) {
            var provider = provider(properties);
            assertSafeFailure(provider, "INVALID_CONTENT");
            verifyNoInteractions(provider.client);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "omitted", "duplicate", "unrequested", "next-token", "shadowed-arn" })
    void malformedBatchesNeverReturnCredentials(String scenario) {
        String arn = "arn:aws:secretsmanager:us-east-1:123456789012:secret:db-AbCdEf";
        String id = scenario.equals("shadowed-arn") ? arn : "db";
        var provider = provider(Map.of("secret-id", id, "batch-retrieval", "true"));
        var value = SecretValueEntry.builder().name("db").arn(arn).secretString("{\"password\":\"secret\"}").build();
        var response = BatchGetSecretValueResponse.builder();
        switch (scenario) {
            case "duplicate" -> response.secretValues(value, value);
            case "unrequested" -> response.secretValues(value, value.toBuilder().name("other").arn("other").build());
            case "shadowed-arn" -> response.secretValues(value,
                    value.toBuilder().name("db-AbCdEf").arn(arn + "-GhIjKl").build());
            case "next-token" -> response.secretValues(value).nextToken("unsafe token");
            default -> {
            }
        }
        when(provider.client.batchGetSecretValue(any(BatchGetSecretValueRequest.class))).thenReturn(response.build());
        assertSafeFailure(provider, "PARTIAL_RESPONSE");
        verify(provider.client, never()).getSecretValue(any(GetSecretValueRequest.class));
    }

    private static void assertSafeFailure(SecretsManagerCredentialsProvider provider, String category) {
        var failure = assertThrows(IllegalStateException.class, () -> provider.getCredentials("db"));
        assertEquals("Unable to load Secrets Manager credentials [" + category + "]", failure.getMessage());
        assertNull(failure.getCause());
        assertEquals(0, failure.getSuppressed().length);
    }

    private static GetSecretValueResponse response(String value) {
        return GetSecretValueResponse.builder().secretString(value).build();
    }

    private static SecretsManagerCredentialsProvider provider() {
        var provider = new SecretsManagerCredentialsProvider();
        provider.config = new SmallRyeConfigBuilder().withMapping(SecretsManagerCredentialsProviderConfig.class)
                .withDefaultValue("quarkus.secretsmanager.credentials-provider.db.secret-id",
                        "arn:aws:secretsmanager:us-east-1:123456789012:secret:database-AbCdEf")
                .build().getConfigMapping(SecretsManagerCredentialsProviderConfig.class);
        provider.client = mock(SecretsManagerClient.class);
        return provider;
    }

    private static SecretsManagerCredentialsProvider provider(Map<String, String> properties) {
        var values = new java.util.HashMap<String, String>();
        properties.forEach((key, value) -> values.put("quarkus.secretsmanager.credentials-provider.db." + key, value));
        var provider = new SecretsManagerCredentialsProvider();
        provider.config = new SmallRyeConfigBuilder().withMapping(SecretsManagerCredentialsProviderConfig.class)
                .withSources(new MapBackedConfigSource("test", values) {
                }).build().getConfigMapping(SecretsManagerCredentialsProviderConfig.class);
        provider.client = mock(SecretsManagerClient.class);
        return provider;
    }
}
