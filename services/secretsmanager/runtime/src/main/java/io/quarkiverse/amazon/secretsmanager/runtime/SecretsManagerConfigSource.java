package io.quarkiverse.amazon.secretsmanager.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import io.quarkiverse.amazon.common.runtime.ConfigSourceFailure;
import io.quarkiverse.amazon.common.runtime.ConfigSourceFormat;
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
    private final ConfigSourceFormat format;

    public SecretsManagerConfigSource(SecretsManagerClient secretsManagerClient, Map<String, String> lookup, int ordinal) {
        this(secretsManagerClient, lookup, ordinal, ConfigSourceFormat.AUTO);
    }

    public SecretsManagerConfigSource(SecretsManagerClient secretsManagerClient, Map<String, String> lookup, int ordinal,
            ConfigSourceFormat format) {
        super(CONFIG_SOURCE_NAME, ordinal);
        this.lookup = lookup;
        this.format = format;

        // Load secrets once; ConfigSource lookups should be fast and side-effect free.
        this.secrets = loadAllSecrets(secretsManagerClient);
    }

    private Map<String, String> loadAllSecrets(SecretsManagerClient client) {
        LOG.info("Loading config values from AWS Secrets Manager...");
        Map<String, String> result = new LinkedHashMap<>();

        try {
            if (!this.lookup.isEmpty()) {
                // Fetch each referenced secret once, even if several properties point to it.
                Map<String, String> values = new HashMap<>();
                for (Map.Entry<String, String> entry : this.lookup.entrySet()) {
                    String value = values.computeIfAbsent(entry.getValue(), id -> getSecretValue(client, id));
                    result.putAll(format.decode(entry.getKey(), value));
                }
            } else {
                for (SecretListEntry entry : listAllSecrets(client)) {
                    result.putAll(format.decode(entry.name(), getSecretValue(client, entry.name())));
                }
            }
        } catch (Exception e) {
            ConfigSourceFailure.classify(e).log(LOG,
                    "Unable to load all Secrets Manager configuration; publishing no partial configuration");
            return Map.of();
        }

        LOG.infof("ConfigSource loaded %d config entries from AWS Secrets Manager.", result.size());
        return Collections.unmodifiableMap(result);
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
