package io.quarkiverse.amazon.secretsmanager.runtime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import io.quarkiverse.amazon.common.runtime.JsonConfigFlattener;
import io.smallrye.config.common.AbstractConfigSource;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ListSecretsRequest;
import software.amazon.awssdk.services.secretsmanager.model.ListSecretsResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretListEntry;

/**
 * A MicroProfile ConfigSource backed by AWS Secrets Manager.
 * <p>
 * At startup, this source lists secrets and fetches their values. Optionally, a lookup map can be
 * provided to expose selected secrets under arbitrary configuration property names. If listing
 * secrets is not permitted (for example, missing {@code secretsmanager:ListSecrets}), listing
 * failures are ignored when a non-empty lookup map is configured and only those referenced secrets
 * are fetched via {@code GetSecretValue}.
 */
public class SecretsManagerConfigSource extends AbstractConfigSource {

    private static final String CONFIG_SOURCE_NAME = "quarkus.secretsmanager.config";
    private static final Logger LOG = Logger.getLogger(SecretsManagerConfigSource.class);
    private final Map<String, String> lookup;
    private final Map<String, String> secrets;

    public SecretsManagerConfigSource(SecretsManagerClient secretsManagerClient, Map<String, String> lookup, int ordinal) {
        super(CONFIG_SOURCE_NAME, ordinal);
        this.lookup = lookup;

        // Load secrets once; ConfigSource lookups should be fast and side-effect free.
        this.secrets = loadAllSecrets(secretsManagerClient);
    }

    private Map<String, String> loadAllSecrets(SecretsManagerClient client) {
        LOG.info("Loading config values from AWS Secrets Manager...");
        Map<String, String> result = new LinkedHashMap<>();

        Iterable<String> secretNames;
        try {
            secretNames = discoverSecretNames(client);
        } catch (Exception e) {
            if (this.lookup.isEmpty()) {
                LOG.errorf(e, "Failed to list secrets from AWS Secrets Manager.");
                return result;
            }
            LOG.warnf(
                    "Failed to list secrets from AWS Secrets Manager; loading only secrets referenced by the lookup map. %s",
                    e.getMessage());
            secretNames = new LinkedHashSet<>(this.lookup.values());
        }

        for (String secretName : secretNames) {
            mergeExpandedSecret(client, secretName, flatteningPrefix(secretName), result);
        }

        LOG.infof("ConfigSource loaded %d config entries from AWS Secrets Manager.", result.size());
        return result;
    }

    private List<String> discoverSecretNames(SecretsManagerClient client) {
        List<String> names = new ArrayList<>();
        for (SecretListEntry entry : listAllSecrets(client)) {
            String secretName = entry.name();
            if (!this.lookup.isEmpty() && !this.lookup.containsValue(secretName)) {
                continue;
            }
            names.add(secretName);
        }
        return names;
    }

    private String flatteningPrefix(String secretName) {
        if (this.lookup.isEmpty()) {
            return secretName;
        }
        return this.lookup.entrySet().stream()
                .filter(e -> e.getValue().equals(secretName))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(secretName);
    }

    private void mergeExpandedSecret(SecretsManagerClient client, String secretName, String prefix,
            Map<String, String> result) {
        try {
            String value = getSecretValue(client, secretName);
            result.putAll(JsonConfigFlattener.expandValue(prefix, value));
        } catch (Exception e) {
            // Handle access denied, deleted secrets, throttling, etc.
            // Keep going so one bad secret doesn't prevent application startup.
            LOG.errorf(e, "Failed to fetch secret '%s' from AWS Secrets Manager.", secretName);
        }
    }

    private List<SecretListEntry> listAllSecrets(SecretsManagerClient client) {
        List<SecretListEntry> secrets = new ArrayList<>();

        String nextToken = null;

        do {
            ListSecretsRequest request = ListSecretsRequest.builder()
                    .nextToken(nextToken)
                    .build();

            ListSecretsResponse response = client.listSecrets(request);
            secrets.addAll(response.secretList());

            nextToken = response.nextToken();

        } while (nextToken != null);

        return secrets;
    }

    private String getSecretValue(SecretsManagerClient client, String secretId) {
        GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(secretId)
                .build();

        GetSecretValueResponse response = client.getSecretValue(request);

        if (response.secretString() != null) {
            return response.secretString();
        } else {
            // Binary secrets are returned as bytes; expose them as UTF-8 text.
            return response.secretBinary().asUtf8String();
        }
    }

    @Override
    public Set<String> getPropertyNames() {
        // Return all flattened keys from the secrets map
        return secrets.keySet();
    }

    @Override
    public String getValue(String propertyName) {
        // Directly look up the property name in the flattened secrets map
        return secrets.get(propertyName);
    }
}
