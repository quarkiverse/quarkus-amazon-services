package io.quarkiverse.amazon.secretsmanager.runtime;

import java.time.Duration;
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
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

/** One GetSecretValue per selected identity; no new batching permission is required. */
public final class SecretsManagerConfigProvider implements AwsConfigProvider {
    private final SecretsManagerClient client;
    private final Set<String> secretIds;

    public SecretsManagerConfigProvider(SecretsManagerClient client, List<String> secretIds) {
        this.client = client;
        this.secretIds = new LinkedHashSet<>(secretIds);
    }

    @Override
    public FetchResult fetchAll() {
        Map<ResourceId, RawDocument> updated = new LinkedHashMap<>();
        Map<ResourceId, FetchFailure> failed = new LinkedHashMap<>();
        secretIds.forEach(id -> fetch(id, updated, failed));
        return new FetchResult(updated, Set.of(), Set.of(), failed, null, Duration.ZERO);
    }

    private void fetch(String secretId, Map<ResourceId, RawDocument> updated, Map<ResourceId, FetchFailure> failed) {
        ResourceId id = new ResourceId(secretId);
        try {
            var response = client.getSecretValue(GetSecretValueRequest.builder().secretId(secretId).build());
            RawDocument raw = response.secretString() != null
                    ? RawDocument.text(id, response.secretString(), null, response.versionId())
                    : new RawDocument(id, response.secretBinary().asByteArray(), null, response.versionId());
            updated.put(id, raw);
        } catch (Exception e) {
            failed.put(id, classify(e));
        }
    }

    private FetchFailure classify(Exception e) {
        return ConfigSourceFailure.classify(e);
    }

    @Override
    public void close() {
        client.close();
    }
}
