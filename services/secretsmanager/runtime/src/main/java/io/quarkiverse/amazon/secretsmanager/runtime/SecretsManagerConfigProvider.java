package io.quarkiverse.amazon.secretsmanager.runtime;

import java.time.Duration;
import java.util.HashSet;
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
import software.amazon.awssdk.services.secretsmanager.model.ListSecretsRequest;

/** One GetSecretValue per selected identity; no new batching permission is required. */
public final class SecretsManagerConfigProvider implements AwsConfigProvider {
    private final SecretsManagerClient client;
    private final Set<String> secretIds;
    private final Set<ResourceId> known = new HashSet<>();

    public SecretsManagerConfigProvider(SecretsManagerClient client, List<String> secretIds) {
        this.client = client;
        this.secretIds = new LinkedHashSet<>(secretIds);
    }

    @Override
    public FetchResult fetchAll() {
        Map<ResourceId, RawDocument> updated = new LinkedHashMap<>();
        Map<ResourceId, FetchFailure> failed = new LinkedHashMap<>();
        Set<ResourceId> selected = new HashSet<>();
        FetchFailure batchFailure = null;
        if (!secretIds.isEmpty()) {
            secretIds.forEach(id -> fetch(id, updated, failed, selected));
        } else {
            // shortcut: legacy list-all selection remains until the explicit-selection migration in step 3.
            try {
                String nextToken = null;
                do {
                    var response = client.listSecrets(ListSecretsRequest.builder().nextToken(nextToken).build());
                    for (var secret : response.secretList()) {
                        if (!selected.contains(new ResourceId(secret.name()))) {
                            fetch(secret.name(), updated, failed, selected);
                        }
                    }
                    nextToken = response.nextToken();
                } while (nextToken != null && !nextToken.isEmpty());
            } catch (Exception e) {
                batchFailure = classify(e);
            }
        }
        Set<ResourceId> removed = new HashSet<>();
        if (batchFailure == null) {
            removed.addAll(known);
            removed.removeAll(selected);
            known.removeAll(removed);
        }
        known.addAll(selected);
        return new FetchResult(updated, Set.of(), removed, failed, batchFailure, Duration.ZERO);
    }

    private void fetch(String secretId, Map<ResourceId, RawDocument> updated, Map<ResourceId, FetchFailure> failed,
            Set<ResourceId> selected) {
        ResourceId id = new ResourceId(secretId);
        selected.add(id);
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
