package io.quarkiverse.amazon.secretsmanager.runtime;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import io.quarkiverse.amazon.common.config.FetchFailure;
import io.quarkiverse.amazon.common.config.RawDocument;
import io.quarkiverse.amazon.common.config.ResourceId;
import io.quarkiverse.amazon.common.runtime.AwsCredentials;
import io.quarkiverse.amazon.common.runtime.AwsCredentials.ReadFailure;
import io.quarkiverse.amazon.common.runtime.ConfigSourceFailure;
import io.quarkus.credentials.CredentialsProvider;
import io.smallrye.common.annotation.Identifier;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

/** Uses the CDI-managed client; each consumer request reads AWSCURRENT without a local credential cache. */
@ApplicationScoped
@Named("secretsmanager-credentials-provider")
// Preserve named lookup without becoming an implicit @Default credentials provider.
@Identifier("secretsmanager-credentials-provider")
public class SecretsManagerCredentialsProvider implements CredentialsProvider {
    @Inject
    SecretsManagerCredentialsProviderConfig config;

    @Inject
    SecretsManagerClient client;

    @Override
    public Map<String, String> getCredentials(String credentialsProviderName) {
        var entry = credentialsProviderName == null ? null : config.credentialsProvider().get(credentialsProviderName);
        if (entry == null) {
            throw new IllegalArgumentException("Unknown Secrets Manager credentials provider.");
        }
        try {
            return AwsCredentials.resolve(entry.secretId(), entry.secrets(), entry.format(), entry.usernameKey().orElse(null),
                    entry.passwordKey(), ids -> read(ids, entry.batchRetrieval()));
        } catch (ReadFailure failure) {
            throw new IllegalStateException("Unable to load Secrets Manager credentials [" + failure.failure() + "]");
        } catch (RuntimeException failure) {
            // SDK and parser exceptions may contain credentials; never propagate their messages or causes.
            throw new IllegalStateException("Unable to load Secrets Manager credentials ["
                    + ConfigSourceFailure.classify(failure) + "]");
        }
    }

    private Map<String, String> read(List<String> ids, boolean batch) {
        Map<ResourceId, RawDocument> documents = new LinkedHashMap<>();
        Map<ResourceId, FetchFailure> failed = new LinkedHashMap<>();
        if (batch) {
            SecretsManagerBatchReader.fetch(client, ids, documents, failed);
        } else {
            for (String id : ids) {
                var response = client.getSecretValue(GetSecretValueRequest.builder().secretId(id)
                        .versionStage("AWSCURRENT").build());
                if ((response.secretString() == null) == (response.secretBinary() == null)) {
                    throw new IllegalArgumentException();
                }
                var resource = new ResourceId(id);
                documents.put(resource, response.secretString() != null
                        ? RawDocument.text(resource, response.secretString(), null, response.versionId())
                        : new RawDocument(resource, response.secretBinary().asByteArray(), null, response.versionId()));
            }
        }
        if (!failed.isEmpty()) {
            throw new ReadFailure(failed.values().iterator().next());
        }
        Map<String, String> values = new LinkedHashMap<>();
        documents.forEach((id, document) -> values.put(id.value(), AwsCredentials.text(document.content())));
        return values;
    }
}
