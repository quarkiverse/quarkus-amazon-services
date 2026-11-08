package io.quarkiverse.amazon.s3.runtime;

import static io.quarkiverse.amazon.s3.runtime.S3ConfigProviderTest.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.amazon.common.runtime.ConfigSourceFormat;
import io.quarkiverse.amazon.s3.runtime.S3ConfigProvider.ObjectLocation;
import io.quarkiverse.amazon.s3.runtime.S3ConfigSource.Selection;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

class S3ConfigSourceTest {
    @ParameterizedTest
    @CsvSource({ "application/json; charset=UTF-8, config.properties, JSON",
            "application/problem+json, config, JSON", "text/json, config.yml, JSON",
            "application/x-yaml, config.json, YAML", "text/yaml, config, YAML",
            "application/vendor+yaml, config, YAML", "text/x-java-properties, config.json, PROPERTIES",
            "application/x-java-properties, config, PROPERTIES", "application/octet-stream, config.JSON, JSON",
            "text/plain, config.properties, PROPERTIES", ", config.YAML, YAML", ", config.yml, YAML" })
    void autoUsesRecognizedMetadataBeforeExtension(String mime, String key, ConfigSourceFormat expected) {
        assertEquals(expected, S3ConfigSource.detectFormat(mime, key));
    }

    @Test
    void sameObjectCanHaveIndependentFormatsAndPrefixesWithOneRead() {
        var client = mock(S3Client.class);
        var object = new ObjectLocation("bucket", "config.properties", null);
        when(client.getObject(any(GetObjectRequest.class))).thenReturn(stream("{\"port\":5432}", "application/json", "e1"));
        var source = new S3ConfigSource(client, List.of(
                new Selection("parsed", object, "db", ConfigSourceFormat.AUTO),
                new Selection("raw", object, "copy", ConfigSourceFormat.RAW)), 1000, Duration.ZERO, 301, true);
        assertEquals("5432", source.getValue("db.port"));
        assertEquals("{\"port\":5432}", source.getValue("copy"));
        assertEquals(301, source.getOrdinal());
        verify(client).getObject(any(GetObjectRequest.class));
        verify(client).close();
    }

    @Test
    void explicitFormatOverridesMetadataAndUnknownFormatsFailSafely() {
        var client = mock(S3Client.class);
        when(client.getObject(any(GetObjectRequest.class))).thenAnswer(call -> stream("host=été", "application/json", null));
        var source = new S3ConfigSource(client, List.of(selection("config", "", ConfigSourceFormat.PROPERTIES)),
                1000, Duration.ZERO, 270, true);
        assertEquals("été", source.getValue("host"));
        when(client.getObject(any(GetObjectRequest.class))).thenAnswer(call -> stream("unsafe=secret", "text/plain", null));
        var failure = assertThrows(IllegalStateException.class, () -> new S3ConfigSource(client,
                List.of(selection("unknown", "", ConfigSourceFormat.AUTO)), 1000, Duration.ZERO, 270, true));
        assertEquals("Unable to load quarkus.s3.config [INVALID_CONTENT]", failure.getMessage());
        assertNull(failure.getCause());
    }

    @Test
    void autoYamlFailsSafelyWithoutTheOptionalYamlExtension() {
        var client = mock(S3Client.class);
        when(client.getObject(any(GetObjectRequest.class))).thenReturn(stream("value: unsafe", "application/yaml", null));
        var failure = assertThrows(IllegalStateException.class, () -> new S3ConfigSource(client,
                List.of(selection("config.yaml", "", ConfigSourceFormat.AUTO)), 1000, Duration.ZERO, 270, true));
        assertEquals("Unable to load quarkus.s3.config [INVALID_CONTENT]", failure.getMessage());
        assertNull(failure.getCause());
        verify(client).close();
    }

    @Test
    void partialCycleRetainsWorkingDocumentsAndMetadataUntilCompletePublication() {
        var client = mock(S3Client.class);
        var a = selection("a.properties", "a", ConfigSourceFormat.AUTO);
        var b = selection("b.json", "b", ConfigSourceFormat.AUTO);
        var cycle = new java.util.concurrent.atomic.AtomicInteger();
        when(client.getObject(any(GetObjectRequest.class))).thenAnswer(call -> {
            GetObjectRequest request = call.getArgument(0);
            int read = cycle.getAndIncrement();
            if (read < 2) {
                return stream("value=old", "text/x-java-properties", "old");
            }
            if (request.key().equals("a.properties")) {
                if (read == 2) {
                    return stream("{\"value\":\"new\"}", "application/json", "new");
                }
                assertEquals("new", request.ifNoneMatch());
                throw S3Exception.builder().statusCode(304).build();
            }
            if (read == 3) {
                throw S3Exception.builder().statusCode(503).message("unsafe").build();
            }
            return stream("value=new", "text/x-java-properties", "new");
        });
        try (var source = new Refreshable(client, List.of(a, b))) {
            assertEquals("old", source.getValue("a.value"));
            source.reload();
            assertEquals("old", source.getValue("a.value"));
            assertEquals("old", source.getValue("b.value"));
            source.reload();
            assertEquals("new", source.getValue("a.value"));
            assertEquals("new", source.getValue("b.value"));
        }
    }

    @Test
    void invalidBodyAndSubsequent304NeverReplaceLastValidSnapshot() {
        var client = mock(S3Client.class);
        when(client.getObject(any(GetObjectRequest.class)))
                .thenReturn(stream("{\"value\":\"old\"}", null, "old"))
                .thenReturn(stream("{unsafe secret", null, "invalid"))
                .thenThrow(S3Exception.builder().statusCode(304).build())
                .thenReturn(stream("{\"value\":\"new\"}", null, "new"));
        try (var source = new Refreshable(client, List.of(selection("config.json", "", ConfigSourceFormat.AUTO)))) {
            source.reload();
            assertEquals("old", source.getValue("value"));
            source.reload();
            assertEquals("old", source.getValue("value"));
            source.reload();
            assertEquals("new", source.getValue("value"));
        }
    }

    @Test
    void deletionRetainsSnapshotAndRecreatedObjectCanRecover() {
        var client = mock(S3Client.class);
        when(client.getObject(any(GetObjectRequest.class)))
                .thenReturn(stream("value=old", null, "old"))
                .thenThrow(S3Exception.builder().statusCode(404).message("unsafe key").build())
                .thenReturn(stream("value=new", null, "new"));
        try (var source = new Refreshable(client, List.of(selection("config.properties", "", ConfigSourceFormat.AUTO)))) {
            source.reload();
            assertEquals("old", source.getValue("value"));
            source.reload();
            assertEquals("new", source.getValue("value"));
        }
    }

    @Test
    void emptyObjectsClearPropertiesAndBadUtf8CannotPublish() {
        var client = mock(S3Client.class);
        when(client.getObject(any(GetObjectRequest.class)))
                .thenReturn(stream("value=old", null, "old"))
                .thenReturn(new ResponseInputStream<>(GetObjectResponse.builder().eTag("invalid").build(),
                        AbortableInputStream.create(new ByteArrayInputStream(new byte[] { (byte) 0xff }))))
                .thenReturn(stream("", null, "empty"));
        try (var source = new Refreshable(client, List.of(selection("config.properties", "", ConfigSourceFormat.AUTO)))) {
            source.reload();
            assertEquals("old", source.getValue("value"));
            source.reload();
            assertTrue(source.getPropertyNames().isEmpty());
        }
    }

    @Test
    void collisionsEvenWithIdenticalValuesFailAndCloseClient() {
        var client = mock(S3Client.class);
        when(client.getObject(any(GetObjectRequest.class))).thenAnswer(call -> stream("password=unsafe", null, "e1"));
        var failure = assertThrows(IllegalStateException.class, () -> new S3ConfigSource(client, List.of(
                selection("a.properties", "db", ConfigSourceFormat.AUTO),
                selection("b.properties", "db", ConfigSourceFormat.AUTO)), 1000, Duration.ZERO, 270, true));
        assertEquals("Unable to load quarkus.s3.config [INVALID_CONTENT]", failure.getMessage());
        assertNull(failure.getCause());
        verify(client).close();
    }

    @Test
    void allPinnedEntriesCloseImmediatelyEvenWhenPollingIsConfigured() {
        var client = mock(S3Client.class);
        when(client.getObject(any(GetObjectRequest.class))).thenReturn(stream("value=pinned", null, "e1"));
        var entry = new Selection("pinned", new ObjectLocation("bucket", "config.properties", "v1"), "",
                ConfigSourceFormat.AUTO);
        try (var source = new Refreshable(client, List.of(entry))) {
            assertEquals("pinned", source.getValue("value"));
            verify(client).close();
            source.reload();
            verify(client).getObject(any(GetObjectRequest.class));
        }
        verify(client, times(1)).close();
    }

    @Test
    void distinctPinnedVersionsOfTheSameKeyHaveIndependentContent() {
        var client = mock(S3Client.class);
        when(client.getObject(any(GetObjectRequest.class))).thenAnswer(call -> {
            GetObjectRequest request = call.getArgument(0);
            return stream("value=" + request.versionId(), null, "etag-" + request.versionId());
        });
        var entries = List.of(
                new Selection("first", new ObjectLocation("bucket", "config.properties", "v1"), "first",
                        ConfigSourceFormat.AUTO),
                new Selection("second", new ObjectLocation("bucket", "config.properties", "v2"), "second",
                        ConfigSourceFormat.AUTO));
        try (var source = new Refreshable(client, entries)) {
            assertEquals("v1", source.getValue("first.value"));
            assertEquals("v2", source.getValue("second.value"));
            source.reload();
            verify(client, times(2)).getObject(any(GetObjectRequest.class));
        }
    }

    static Selection selection(String key, String prefix, ConfigSourceFormat format) {
        return new Selection(key, new ObjectLocation("bucket", key, null), prefix, format);
    }

    private static class Refreshable extends S3ConfigSource {
        Refreshable(S3Client client, List<Selection> entries) {
            super(client, entries, 1000, Duration.ofHours(1), 270, true);
        }

        void reload() {
            refresh();
        }
    }
}
