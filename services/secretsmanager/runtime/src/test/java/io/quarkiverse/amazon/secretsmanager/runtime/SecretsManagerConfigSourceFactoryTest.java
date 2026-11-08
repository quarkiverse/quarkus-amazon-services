package io.quarkiverse.amazon.secretsmanager.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.BatchGetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.BatchGetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;
import software.amazon.awssdk.services.secretsmanager.model.SecretValueEntry;

class SecretsManagerConfigSourceFactoryTest {
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void namedAndLegacyEntriesDecodeTheSameSecretIndependentlyWithOneRead(boolean batch) {
        var mapping = new SmallRyeConfigBuilder().withMapping(SecretsManagerConfigConfig.class)
                .withDefaultValue("quarkus.secretsmanager.config.enabled", "true")
                .withDefaultValue("quarkus.secretsmanager.config.batch-retrieval", Boolean.toString(batch))
                .withSources(new MapBackedConfigSource("test", Map.of(
                        "quarkus.secretsmanager.config.format", "raw",
                        "quarkus.secretsmanager.config.secrets.opaque", "shared",
                        "quarkus.secretsmanager.config.entries.db.secret-id", "shared",
                        "quarkus.secretsmanager.config.entries.db.prefix", "db",
                        "quarkus.secretsmanager.config.entries.db.format", "json")) {
                })
                .build().getConfigMapping(SecretsManagerConfigConfig.class);
        var client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(
                GetSecretValueResponse.builder().secretString("{\"password\":\"value\"}").build());
        when(client.batchGetSecretValue(any(BatchGetSecretValueRequest.class))).thenReturn(
                BatchGetSecretValueResponse.builder().secretValues(
                        SecretValueEntry.builder().name("shared").secretString("{\"password\":\"value\"}").build()).build());
        var factory = new SecretsManagerConfigSourceFactory() {
            SecretsManagerConfig getClientConfig(ConfigSourceContext context) {
                return null;
            }

            SecretsManagerClient build(SecretsManagerConfig config, ConfigSourceContext context) {
                return client;
            }
        };
        var source = factory.getConfigSources(null, mapping).iterator().next();
        assertEquals("{\"password\":\"value\"}", source.getValue("opaque"));
        assertEquals("value", source.getValue("db.password"));
        verify(client, times(batch ? 0 : 1)).getSecretValue(any(GetSecretValueRequest.class));
        verify(client, times(batch ? 1 : 0)).batchGetSecretValue(any(BatchGetSecretValueRequest.class));
        verify(client).close();
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void emptySelectionNeverBuildsAClientOrListsTheAccount(boolean required) {
        var mapping = new SmallRyeConfigBuilder().withMapping(SecretsManagerConfigConfig.class)
                .withDefaultValue("quarkus.secretsmanager.config.enabled", "true")
                .withDefaultValue("quarkus.secretsmanager.config.fail-on-missing-config", Boolean.toString(required))
                .build().getConfigMapping(SecretsManagerConfigConfig.class);
        var factory = new SecretsManagerConfigSourceFactory();
        if (required) {
            assertThrows(IllegalArgumentException.class, () -> factory.getConfigSources(null, mapping));
        } else {
            assertTrue(!factory.getConfigSources(null, mapping).iterator().hasNext());
        }
    }

    @ParameterizedTest
    @CsvSource({ "false, false", "false, true", "true, false", "true, true" })
    void factoryAppliesStartupPolicyAndOrdinal(boolean required, boolean batch) {
        var mapping = new SmallRyeConfigBuilder().withMapping(SecretsManagerConfigConfig.class)
                .withDefaultValue("quarkus.secretsmanager.config.enabled", "true")
                .withDefaultValue("quarkus.secretsmanager.config.batch-retrieval", Boolean.toString(batch))
                .withDefaultValue("quarkus.secretsmanager.config.secrets.password", "missing")
                .withDefaultValue("quarkus.secretsmanager.config.ordinal", "301")
                .withDefaultValue("quarkus.secretsmanager.config.fail-on-missing-config", Boolean.toString(required))
                .build().getConfigMapping(SecretsManagerConfigConfig.class);
        var client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(GetSecretValueRequest.class))).thenThrow(
                ResourceNotFoundException.builder().statusCode(404).message("unsafe secret").build());
        when(client.batchGetSecretValue(any(BatchGetSecretValueRequest.class))).thenReturn(
                BatchGetSecretValueResponse.builder().errors(software.amazon.awssdk.services.secretsmanager.model.APIErrorType
                        .builder().secretId("missing").errorCode("ResourceNotFoundException").message("unsafe secret").build())
                        .build());
        var factory = new SecretsManagerConfigSourceFactory() {
            SecretsManagerConfig getClientConfig(ConfigSourceContext context) {
                return null;
            }

            SecretsManagerClient build(SecretsManagerConfig config, ConfigSourceContext context) {
                return client;
            }
        };
        if (required) {
            var failure = assertThrows(IllegalStateException.class, () -> factory.getConfigSources(null, mapping));
            assertEquals("Unable to load quarkus.secretsmanager.config [MISSING]", failure.getMessage());
        } else {
            var source = factory.getConfigSources(null, mapping).iterator().next();
            assertEquals(301, source.getOrdinal());
            assertTrue(source.getPropertyNames().isEmpty());
        }
        verify(client).close();
    }

    @Test
    void batchingIsDisabledByDefaultAndDisabledSourceDoesNotRead() {
        var config = new SmallRyeConfigBuilder().withMapping(SecretsManagerConfigConfig.class).build()
                .getConfigMapping(SecretsManagerConfigConfig.class);
        assertEquals(false, config.batchRetrieval());
        assertTrue(!new SecretsManagerConfigSourceFactory().getConfigSources(null, config).iterator().hasNext());
    }
}
