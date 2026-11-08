package io.quarkiverse.amazon.s3.runtime;

import static io.quarkiverse.amazon.s3.runtime.S3ConfigProviderTest.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.EnvConfigSource;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

class S3ConfigSourceFactoryTest {
    @ParameterizedTest
    @CsvSource({ "properties,,5432", "json,properties,5432", "json,auto,5432" })
    void entryFormatOverridesGlobalDefaultIncludingExplicitAuto(String global, String override, String expected) {
        var values = new java.util.HashMap<String, String>();
        values.put("quarkus.s3.config.format", global);
        if (override != null) {
            values.put("quarkus.s3.config.entries.app.format", override);
        }
        var client = mock(S3Client.class);
        when(client.getObject(any(GetObjectRequest.class))).thenReturn(
                stream("port=5432", "application/octet-stream", "e1"));
        var source = factory(client).getConfigSources(null, entryMapping(values)).iterator().next();
        assertEquals(expected, source.getValue("port"));
        verify(client).close();
    }

    @Test
    void versionPinCanBeProvidedDirectlyByAnEnvironmentVariable() {
        var config = new SmallRyeConfigBuilder().withMapping(S3ConfigSourceConfig.class)
                .withDefaultValue("quarkus.s3.config.enabled", "true")
                .withDefaultValue("quarkus.s3.config.entries.\"app\".bucket", "bucket")
                .withDefaultValue("quarkus.s3.config.entries.\"app\".key", "application.properties")
                .withDefaultValue("quarkus.s3.config.entries.\"app\".version-id", "")
                .withSources(new EnvConfigSource(Map.of("QUARKUS_S3_CONFIG_ENTRIES__APP__VERSION_ID", "v-env"), 300))
                .build().getConfigMapping(S3ConfigSourceConfig.class);
        assertEquals("v-env", config.entries().get("app").versionId().orElseThrow());
    }

    @Test
    void disabledSourceDoesNotCreateAClientOrRequireEntries() {
        var client = mock(S3Client.class);
        assertTrue(!factory(client).getConfigSources(null, mapping(Map.of("quarkus.s3.config.enabled", "false")))
                .iterator().hasNext());
        verifyNoInteractions(client);
    }

    @Test
    void emptySelectionNeverListsBucketsEvenWhenOptional() {
        var client = mock(S3Client.class);
        assertThrows(IllegalArgumentException.class, () -> factory(client).getConfigSources(null, mapping(Map.of())));
        assertTrue(!factory(client).getConfigSources(null,
                mapping(Map.of("quarkus.s3.config.fail-on-missing-config", "false"))).iterator().hasNext());
        verifyNoInteractions(client);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void appliesStartupFailurePolicyAndOrdinal(boolean required) {
        var client = mock(S3Client.class);
        when(client.getObject(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(403).message("unsafe bucket/key").build());
        var mapping = entryMapping(Map.of("quarkus.s3.config.ordinal", "301",
                "quarkus.s3.config.fail-on-missing-config", Boolean.toString(required)));
        if (required) {
            var failure = assertThrows(IllegalStateException.class, () -> factory(client).getConfigSources(null, mapping));
            assertEquals("Unable to load quarkus.s3.config [ACCESS_DENIED]", failure.getMessage());
            assertNull(failure.getCause());
        } else {
            var source = factory(client).getConfigSources(null, mapping).iterator().next();
            assertEquals(301, source.getOrdinal());
            assertTrue(source.getPropertyNames().isEmpty());
        }
        verify(client).close();
    }

    @ParameterizedTest
    @CsvSource({ "max-object-size, 0", "max-object-size, 2G", "update-interval, -1S", "update-interval, 100ms" })
    void invalidSettingsFailBeforeCreatingClientEvenIfSourceIsOptional(String property, String value) {
        var client = mock(S3Client.class);
        var config = entryMapping(Map.of("quarkus.s3.config." + property, value,
                "quarkus.s3.config.fail-on-missing-config", "false"));
        var failure = assertThrows(IllegalArgumentException.class, () -> factory(client).getConfigSources(null, config));
        assertTrue(failure.getMessage().contains(property));
        verifyNoInteractions(client);
    }

    @Test
    void mappingSupportsPrefixesPinningAndCustomSize() {
        var client = mock(S3Client.class);
        when(client.getObject(any(GetObjectRequest.class))).thenReturn(stream("port=5432", "application/json", "e1"));
        var config = entryMapping(Map.of("quarkus.s3.config.entries.app.version-id", "v1",
                "quarkus.s3.config.entries.app.prefix", "myapp",
                "quarkus.s3.config.entries.app.format", "properties",
                "quarkus.s3.config.max-object-size", "2M", "quarkus.s3.config.update-interval", "1S"));
        assertEquals(2097152L, config.maxObjectSize().asLongValue());
        var source = factory(client).getConfigSources(null, config).iterator().next();
        assertEquals("5432", source.getValue("myapp.port"));
        var request = org.mockito.ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(client).getObject(request.capture());
        assertEquals("v1", request.getValue().versionId());
        verify(client).close();
    }

    @Test
    void mutableNullVersionIsNotAcceptedAsAnImmutablePin() {
        var client = mock(S3Client.class);
        var config = entryMapping(Map.of("quarkus.s3.config.entries.app.version-id", "null"));
        assertThrows(IllegalArgumentException.class, () -> factory(client).getConfigSources(null, config));
        verifyNoInteractions(client);
    }

    @Test
    void realBootstrapClientUsesServiceSettingsIncludingPathStyle() {
        var properties = new MapBackedConfigSource("test", Map.of(
                "quarkus.s3.aws.region", "us-east-1", "quarkus.s3.aws.credentials.type", "anonymous",
                "quarkus.s3.endpoint-override", "http://localhost:9000", "quarkus.s3.path-style-access", "true",
                "quarkus.s3.api-call-timeout", "3S", "quarkus.s3.api-call-attempt-timeout", "1S",
                "quarkus.s3.sync-client.type", "url")) {
        };
        var config = new SmallRyeConfigBuilder().withSources(properties).build();
        var context = new ConfigSourceContext() {
            public ConfigValue getValue(String name) {
                return config.getConfigValue(name);
            }

            public Iterator<String> iterateNames() {
                return config.getPropertyNames().iterator();
            }

            public List<ConfigSource> getConfigSources() {
                return List.of(properties);
            }

            public List<String> getProfiles() {
                return List.of();
            }
        };
        var factory = new S3ConfigSourceFactory();
        try (var client = factory.build(factory.getClientConfig(context), context)) {
            assertEquals("http://localhost:9000/bucket/key",
                    client.utilities().getUrl(b -> b.bucket("bucket").key("key")).toString());
            var overrides = client.serviceClientConfiguration().overrideConfiguration();
            assertEquals(Duration.ofSeconds(3), overrides.apiCallTimeout().orElseThrow());
            assertEquals(Duration.ofSeconds(1), overrides.apiCallAttemptTimeout().orElseThrow());
        }
    }

    private static S3ConfigSourceConfig entryMapping(Map<String, String> properties) {
        var values = new java.util.HashMap<>(properties);
        values.put("quarkus.s3.config.entries.app.bucket", "bucket");
        values.put("quarkus.s3.config.entries.app.key", "application.properties");
        return mapping(values);
    }

    private static S3ConfigSourceConfig mapping(Map<String, String> properties) {
        return new SmallRyeConfigBuilder().withMapping(S3ConfigSourceConfig.class)
                .withDefaultValue("quarkus.s3.config.enabled", "true")
                .withSources(new MapBackedConfigSource("test", properties) {
                }).build().getConfigMapping(S3ConfigSourceConfig.class);
    }

    private static S3ConfigSourceFactory factory(S3Client client) {
        return new S3ConfigSourceFactory() {
            S3Config getClientConfig(ConfigSourceContext context) {
                return null;
            }

            S3Client build(S3Config config, ConfigSourceContext context) {
                return client;
            }
        };
    }
}
