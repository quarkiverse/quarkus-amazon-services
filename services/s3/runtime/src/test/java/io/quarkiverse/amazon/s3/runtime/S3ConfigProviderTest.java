package io.quarkiverse.amazon.s3.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.quarkiverse.amazon.common.config.FetchFailure;
import io.quarkiverse.amazon.s3.runtime.S3ConfigProvider.ObjectLocation;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

class S3ConfigProviderTest {
    @Test
    void conditionalGetsPreserveOpaqueEtagsAndDeduplicateObjects() {
        var client = mock(S3Client.class);
        var object = new ObjectLocation("bucket", "folder/a +%?#.json", null);
        when(client.getObject(any(GetObjectRequest.class)))
                .thenReturn(stream("{}", "application/json", "\"multipart-2\""))
                .thenThrow(S3Exception.builder().statusCode(304).message("unsafe").build());
        try (var provider = new S3ConfigProvider(client, List.of(object, object), 100)) {
            assertEquals(1, provider.fetchAll().updated().size());
            var second = provider.fetchAll();
            assertEquals(java.util.Set.of(object.resource()), second.unchanged());
            assertTrue(second.failed().isEmpty());
            assertEquals("application/json", provider.contentType(object.resource()));
            var requests = ArgumentCaptor.forClass(GetObjectRequest.class);
            verify(client, times(2)).getObject(requests.capture());
            assertNull(requests.getAllValues().get(0).ifNoneMatch());
            var conditional = requests.getAllValues().get(1);
            assertEquals("bucket", conditional.bucket());
            assertEquals("folder/a +%?#.json", conditional.key());
            assertEquals("\"multipart-2\"", conditional.ifNoneMatch());
            assertTrue(conditional.overrideConfiguration().isEmpty());
        }
        verify(client).close();
        verifyNoMoreInteractions(client);
    }

    @Test
    void pinnedObjectsAreNotRereadWhileCurrentObjectsStillRefresh() {
        var client = mock(S3Client.class);
        var pinned = new ObjectLocation("bucket", "config.json", "v:1?/#");
        var current = new ObjectLocation("bucket", "config.json", null);
        when(client.getObject(any(GetObjectRequest.class))).thenAnswer(call -> stream("{}", null, null));
        try (var provider = new S3ConfigProvider(client, List.of(pinned, current), 100)) {
            assertEquals(2, provider.fetchAll().updated().size());
            var second = provider.fetchAll();
            assertEquals(java.util.Set.of(pinned.resource()), second.unchanged());
            assertEquals(java.util.Set.of(current.resource()), second.updated().keySet());
            var requests = ArgumentCaptor.forClass(GetObjectRequest.class);
            verify(client, times(3)).getObject(requests.capture());
            assertEquals("v:1?/#", requests.getAllValues().get(0).versionId());
            assertNull(requests.getAllValues().get(1).versionId());
            assertNull(requests.getAllValues().get(2).versionId());
        }
    }

    @Test
    void failedPinnedReadsCanRetryButNeverBecomeUnchangedBeforeSuccess() {
        var client = mock(S3Client.class);
        var object = new ObjectLocation("bucket", "config.json", "v1");
        when(client.getObject(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(503).message("unsafe").build())
                .thenReturn(stream("{}", null, "e1"));
        try (var provider = new S3ConfigProvider(client, List.of(object), 100)) {
            assertEquals(FetchFailure.TRANSIENT, provider.fetchAll().failed().get(object.resource()));
            assertEquals(1, provider.fetchAll().updated().size());
            assertEquals(java.util.Set.of(object.resource()), provider.fetchAll().unchanged());
            verify(client, times(2)).getObject(any(GetObjectRequest.class));
        }
    }

    @Test
    void readLimitIsEnforcedWithoutContentLengthAndAbortsRatherThanDrains() {
        var client = mock(S3Client.class);
        var object = new ObjectLocation("bucket", "config.json", null);
        var aborted = new AtomicBoolean();
        var bytesRead = new java.util.concurrent.atomic.AtomicInteger();
        InputStream endless = new InputStream() {
            public int read() {
                bytesRead.incrementAndGet();
                return 'a';
            }
        };
        when(client.getObject(any(GetObjectRequest.class))).thenReturn(new ResponseInputStream<>(
                GetObjectResponse.builder().eTag("too-large").build(),
                AbortableInputStream.create(endless, () -> aborted.set(true))));
        try (var provider = new S3ConfigProvider(client, List.of(object), 8)) {
            assertEquals(FetchFailure.INVALID_CONTENT, provider.fetchAll().failed().get(object.resource()));
            assertEquals(9, bytesRead.get());
            assertTrue(aborted.get());
        }
    }

    @Test
    void excessiveContentLengthRejectsBeforeReadingAndDoesNotAdvanceEtag() throws Exception {
        var client = mock(S3Client.class);
        var object = new ObjectLocation("bucket", "config.json", null);
        var input = mock(InputStream.class);
        var aborted = new AtomicBoolean();
        when(client.getObject(any(GetObjectRequest.class))).thenReturn(new ResponseInputStream<>(
                GetObjectResponse.builder().contentLength(101L).eTag("bad").build(),
                AbortableInputStream.create(input, () -> aborted.set(true))))
                .thenReturn(stream("{}", null, "good"));
        try (var provider = new S3ConfigProvider(client, List.of(object), 100)) {
            assertEquals(FetchFailure.INVALID_CONTENT, provider.fetchAll().failed().get(object.resource()));
            assertTrue(aborted.get());
            verify(input, never()).read();
            assertEquals(1, provider.fetchAll().updated().size());
            var requests = ArgumentCaptor.forClass(GetObjectRequest.class);
            verify(client, times(2)).getObject(requests.capture());
            assertNull(requests.getAllValues().get(1).ifNoneMatch());
        }
    }

    @Test
    void truncatedBodiesAndCloseFailuresDoNotAdvanceTransportState() {
        var client = mock(S3Client.class);
        var object = new ObjectLocation("bucket", "config.json", null);
        var brokenClose = new ByteArrayInputStream(new byte[0]) {
            public void close() throws IOException {
                throw new IOException("unsafe close");
            }
        };
        when(client.getObject(any(GetObjectRequest.class)))
                .thenReturn(new ResponseInputStream<>(GetObjectResponse.builder().contentLength(10L).build(),
                        AbortableInputStream.create(new ByteArrayInputStream(new byte[2]))))
                .thenReturn(new ResponseInputStream<>(GetObjectResponse.builder().eTag("bad").build(),
                        AbortableInputStream.create(brokenClose)))
                .thenReturn(stream("{}", null, "good"));
        try (var provider = new S3ConfigProvider(client, List.of(object), 100)) {
            for (int i = 0; i < 2; i++) {
                var failure = provider.fetchAll();
                assertEquals(FetchFailure.TRANSIENT, failure.failed().get(object.resource()));
                assertTrue(failure.updated().isEmpty());
            }
            assertEquals(1, provider.fetchAll().updated().size());
            var requests = ArgumentCaptor.forClass(GetObjectRequest.class);
            verify(client, times(3)).getObject(requests.capture());
            assertTrue(requests.getAllValues().stream().allMatch(request -> request.ifNoneMatch() == null));
        }
    }

    @Test
    void failuresArePerObjectAndNeverMistakenForDeletionsOrUnchangedData() {
        var client = mock(S3Client.class);
        var missing = new ObjectLocation("bucket", "missing.json", null);
        var denied = new ObjectLocation("bucket", "denied.json", null);
        var unexpected304 = new ObjectLocation("bucket", "unknown.json", null);
        when(client.getObject(any(GetObjectRequest.class))).thenAnswer(call -> {
            GetObjectRequest request = call.getArgument(0);
            int status = request.key().equals("missing.json") ? 404 : request.key().equals("denied.json") ? 403 : 304;
            throw S3Exception.builder().statusCode(status).message("unsafe key/content").build();
        });
        try (var provider = new S3ConfigProvider(client, List.of(missing, denied, unexpected304), 100)) {
            var result = provider.fetchAll();
            assertEquals(FetchFailure.MISSING, result.failed().get(missing.resource()));
            assertEquals(FetchFailure.ACCESS_DENIED, result.failed().get(denied.resource()));
            assertEquals(FetchFailure.OTHER, result.failed().get(unexpected304.resource()));
            assertTrue(result.removed().isEmpty());
            assertTrue(result.unchanged().isEmpty());
        }
    }

    @Test
    void resourceIdentityDistinguishesVersionsAndOpaqueKeySeparators() {
        assertNotEquals(new ObjectLocation("bucket", "a", "bc").resource(),
                new ObjectLocation("bucket", "ab", "c").resource());
        assertNotEquals(new ObjectLocation("bucket", "a", null).resource(),
                new ObjectLocation("bucket", "a", "v1").resource());
        assertFalse(new ObjectLocation("bucket", "a", null).equals(new ObjectLocation("bucket", "a", "v1")));
        assertThrows(IllegalArgumentException.class, () -> new ObjectLocation("bucket", "a", "null"));
    }

    static ResponseInputStream<GetObjectResponse> stream(String text, String contentType, String etag) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        return new ResponseInputStream<>(GetObjectResponse.builder().contentLength((long) bytes.length)
                .contentType(contentType).eTag(etag).build(), AbortableInputStream.create(new ByteArrayInputStream(bytes)));
    }
}
