package io.quarkiverse.amazon.secretsmanager.runtime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
 * provided to expose selected secrets under arbitrary configuration property names.
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

        List<SecretListEntry> secrets;
        try {
            secrets = listAllSecrets(client);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to list secrets from AWS Secrets Manager.");
            return result;
        }

        for (SecretListEntry entry : secrets) {
            String secretName = entry.name();

            // When a lookup mapping is provided, only fetch secrets referenced by the mapping values.
            if (!this.lookup.isEmpty() && !this.lookup.containsValue(secretName)) {
                continue;
            }

            try {
                String value = getSecretValue(client, secretName);

                // Find the prefix to use for flattening
                // If the secret is mapped, use the mapped property name as prefix
                // Otherwise, use the secret name
                String prefix;
                if (!this.lookup.isEmpty()) {
                    // Find the mapped property name for this secret
                    prefix = this.lookup.entrySet().stream()
                            .filter(e -> e.getValue().equals(secretName))
                            .map(Map.Entry::getKey)
                            .findFirst()
                            .orElse(secretName);
                } else {
                    prefix = secretName;
                }

                // Expand the secret value (flatten if JSON)
                Map<String, String> expanded = JsonConfigFlattener.expandValue(prefix, value);
                result.putAll(expanded);

            } catch (Exception e) {
                // Handle access denied, deleted secrets, throttling, etc.
                // Keep going so one bad secret doesn't prevent application startup.
                LOG.errorf(e, "Failed to fetch secret '%s' from AWS Secrets Manager.", secretName);
            }
        }

        LOG.infof("ConfigSource loaded %d config entries from AWS Secrets Manager.", result.size());
        return result;
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
