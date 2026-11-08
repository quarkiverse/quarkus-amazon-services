package io.quarkiverse.amazon.secretsmanager.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.amazon.common.runtime.ConfigSourceFormat;
import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ListSecretsRequest;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;

/**
 * Lookup mode of {@link SecretsManagerConfigSource}. JSON flattening is covered by {@code JsonConfigFlattenerTest}.
 */
public class SecretsManagerConfigSourceTest {

    private static SecretsManagerClient clientWithSecret(String secretId, String value) {
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(GetSecretValueRequest.class))).thenAnswer(inv -> {
            GetSecretValueRequest request = inv.getArgument(0);
            if (!secretId.equals(request.secretId())) {
                throw ResourceNotFoundException.builder().message("not found").build();
            }
            return GetSecretValueResponse.builder().secretString(value).build();
        });
        return client;
    }

    @Test
    void testTwoPropertiesMappedToSameSecret() {
        SecretsManagerClient client = clientWithSecret("shared", "s3cr3t");
        Map<String, String> lookup = new LinkedHashMap<>();
        lookup.put("a.password", "shared");
        lookup.put("b.password", "shared");

        var source = new SecretsManagerConfigSource(client, lookup, 270);

        assertEquals("s3cr3t", source.getValue("a.password"));
        assertEquals("s3cr3t", source.getValue("b.password"));
        verify(client, times(1)).getSecretValue(any(GetSecretValueRequest.class));
        verify(client, never()).listSecrets(any(ListSecretsRequest.class));
    }

    @Test
    void testArnLookupValueDoesNotListSecrets() {
        String arn = "arn:aws:secretsmanager:us-east-1:123456789012:secret:db-AbCdEf";
        SecretsManagerClient client = clientWithSecret(arn, "{\"host\":\"localhost\"}");

        var source = new SecretsManagerConfigSource(client, Map.of("db", arn), 270);

        assertEquals("localhost", source.getValue("db.host"));
        verify(client, never()).listSecrets(any(ListSecretsRequest.class));
    }

    @ParameterizedTest
    @ValueSource(strings = { "null", "true", "123", "  \"abc\"  ", "{}", "[]", "{\"a\":null}", "dGVzdA==" })
    void sourcePreservesCompatibleAutoValues(String value) {
        var source = new SecretsManagerConfigSource(clientWithSecret("secret", value), Map.of("target", "secret"), 270);
        assertEquals(value, source.getValue("target"));
    }

    @Test
    void sourceUsesDeclaredPropertiesFormat() {
        var source = new SecretsManagerConfigSource(clientWithSecret("secret", "url=x\nuser=y"), Map.of("db", "secret"),
                270, ConfigSourceFormat.PROPERTIES);
        assertEquals("x", source.getValue("db.url"));
        assertEquals("y", source.getValue("db.user"));
    }

    @Test
    void unknownFormatFailsConfigurationMapping() {
        assertThrows(ConfigValidationException.class,
                () -> new SmallRyeConfigBuilder().withMapping(SecretsManagerConfigConfig.class)
                        .withSources(
                                new MapBackedConfigSource("test", Map.of("quarkus.secretsmanager.config.format", "invalid")) {
                                })
                        .build());
    }

    @Test
    void failureAfterOneSecretPublishesNoPartialConfiguration() {
        SecretsManagerClient client = clientWithSecret("good", "value");
        Map<String, String> lookup = new LinkedHashMap<>();
        lookup.put("a", "good");
        lookup.put("b", "missing");
        var source = new SecretsManagerConfigSource(client, lookup, 270);
        assertTrue(source.getPropertyNames().isEmpty());
    }

    @Test
    void binaryUtf8IsPreservedAndInvalidEncodingRejectsTheWholeLoad() {
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(
                GetSecretValueResponse.builder().secretBinary(SdkBytes.fromUtf8String("été")).build(),
                GetSecretValueResponse.builder().secretBinary(SdkBytes.fromByteArray(new byte[] { (byte) 0xff, (byte) 0xfe }))
                        .build());
        assertEquals("été", new SecretsManagerConfigSource(client, Map.of("text", "secret"), 270).getValue("text"));
        assertTrue(new SecretsManagerConfigSource(client, Map.of("text", "secret"), 270).getPropertyNames().isEmpty());
    }

    @Test
    void invalidDeclaredJsonPublishesNoPartialConfiguration() {
        SecretsManagerClient client = clientWithSecret("secret", "{invalid json");
        var source = new SecretsManagerConfigSource(client, Map.of("target", "secret"), 270, ConfigSourceFormat.JSON);
        assertTrue(source.getPropertyNames().isEmpty());
    }
}
