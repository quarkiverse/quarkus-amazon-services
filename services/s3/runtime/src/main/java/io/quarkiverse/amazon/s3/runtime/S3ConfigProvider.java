package io.quarkiverse.amazon.s3.runtime;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkiverse.amazon.common.config.AwsConfigProvider;
import io.quarkiverse.amazon.common.config.FetchFailure;
import io.quarkiverse.amazon.common.config.FetchResult;
import io.quarkiverse.amazon.common.config.RawDocument;
import io.quarkiverse.amazon.common.config.ResourceId;
import io.quarkiverse.amazon.common.runtime.ConfigSourceFailure;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** Owns conditional-read state and the bootstrap client; publication belongs to the shared snapshot. */
public final class S3ConfigProvider implements AwsConfigProvider {
    public record ObjectLocation(String bucket, String key, String versionId) {
        public ObjectLocation {
            if (bucket == null || bucket.isBlank() || key == null || key.isBlank()
                    || versionId != null && versionId.isBlank()) {
                throw new IllegalArgumentException("S3 configuration requires non-blank bucket, key and optional version-id.");
            }
            if ("null".equals(versionId)) {
                throw new IllegalArgumentException(
                        "S3 configuration version-id must identify an immutable version, not the mutable null version.");
            }
        }

        public ResourceId resource() {
            // Length delimiters keep keys and version IDs opaque, without separator collisions.
            return new ResourceId(bucket.length() + ":" + bucket + key.length() + ":" + key
                    + (versionId == null ? "" : versionId));
        }
    }

    private final S3Client client;
    private final Set<ObjectLocation> objects;
    private final int maxObjectSize;
    private final Map<ResourceId, GetObjectResponse> received = new HashMap<>();

    public S3ConfigProvider(S3Client client, List<ObjectLocation> objects, int maxObjectSize) {
        this.client = client;
        this.objects = new LinkedHashSet<>(objects);
        this.maxObjectSize = maxObjectSize;
    }

    String contentType(ResourceId id) {
        return received.get(id).contentType();
    }

    @Override
    public FetchResult fetchAll() {
        Map<ResourceId, RawDocument> updated = new LinkedHashMap<>();
        Map<ResourceId, FetchFailure> failed = new LinkedHashMap<>();
        Set<ResourceId> unchanged = new LinkedHashSet<>();
        for (var object : objects) {
            var id = object.resource();
            var previous = received.get(id);
            if (object.versionId() != null && previous != null) {
                unchanged.add(id);
                continue;
            }
            var request = GetObjectRequest.builder().bucket(object.bucket()).key(object.key()).versionId(object.versionId());
            if (previous != null && previous.eTag() != null) {
                request.ifNoneMatch(previous.eTag());
            }
            try {
                GetObjectResponse response;
                byte[] content;
                try (var stream = client.getObject(request.build())) {
                    response = stream.response();
                    try {
                        if (response.contentLength() != null && response.contentLength() > maxObjectSize) {
                            throw new IllegalArgumentException("S3 configuration object exceeds max-object-size.");
                        }
                        content = stream.readNBytes(maxObjectSize + 1);
                        if (content.length > maxObjectSize) {
                            throw new IllegalArgumentException("S3 configuration object exceeds max-object-size.");
                        }
                        if (response.contentLength() != null && response.contentLength() != content.length) {
                            throw new IOException("Incomplete S3 configuration object.");
                        }
                    } catch (IOException | RuntimeException failure) {
                        stream.abort();
                        throw failure;
                    }
                }
                updated.put(id, new RawDocument(id, content, response.contentType(), response.versionId()));
                received.put(id, response);
            } catch (S3Exception failure) {
                if (failure.statusCode() == 304 && previous != null && previous.eTag() != null) {
                    unchanged.add(id);
                } else {
                    failed.put(id, ConfigSourceFailure.classify(failure));
                }
            } catch (IOException failure) {
                failed.put(id, FetchFailure.TRANSIENT);
            } catch (Exception failure) {
                failed.put(id, ConfigSourceFailure.classify(failure));
            }
        }
        return new FetchResult(updated, unchanged, Set.of(), failed, null, Duration.ZERO);
    }

    @Override
    public void close() {
        client.close();
    }
}
