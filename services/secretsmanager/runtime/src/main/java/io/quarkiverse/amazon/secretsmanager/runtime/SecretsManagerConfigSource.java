package io.quarkiverse.amazon.secretsmanager.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

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
 * At startup, when a lookup map (property name to secret id) is configured, each referenced secret is
 * fetched via {@code GetSecretValue} and exposed under its property name; the secret id may be a name
 * or an ARN, and {@code secretsmanager:ListSecrets} is not needed. Without a lookup map, all secrets are
 * listed and exposed under their secret name.
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

        if (!this.lookup.isEmpty()) {
            // Fetch each referenced secret once, even if several properties point to it.
            Map<String, String> values = new HashMap<>();
            for (Map.Entry<String, String> entry : this.lookup.entrySet()) {
                mergeExpandedSecret(entry.getValue(), entry.getKey(), result,
                        id -> values.computeIfAbsent(id, i -> getSecretValue(client, i)));
            }
        } else {
            List<SecretListEntry> secrets;
            try {
                secrets = listAllSecrets(client);
            } catch (Exception e) {
                LOG.errorf(e, "Failed to list secrets from AWS Secrets Manager.");
                return result;
            }
            for (SecretListEntry entry : secrets) {
                mergeExpandedSecret(entry.name(), entry.name(), result, id -> getSecretValue(client, id));
            }
        }

        LOG.infof("ConfigSource loaded %d config entries from AWS Secrets Manager.", result.size());
        return result;
    }

    private void mergeExpandedSecret(String secretName, String prefix, Map<String, String> result,
            Function<String, String> fetcher) {
        try {
            result.putAll(JsonConfigFlattener.expandValue(prefix, fetcher.apply(secretName)));
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
