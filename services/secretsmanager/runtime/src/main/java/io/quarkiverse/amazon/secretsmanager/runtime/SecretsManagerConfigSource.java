package io.quarkiverse.amazon.secretsmanager.runtime;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.quarkiverse.amazon.common.config.AbstractAwsConfigSource;
import io.quarkiverse.amazon.common.config.ConfigSourceEntry;
import io.quarkiverse.amazon.common.config.ResourceId;
import io.quarkiverse.amazon.common.runtime.ConfigSourceFormat;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

/** Loads a complete startup snapshot, fetching each mapped secret only once. Owns the supplied client. */
public class SecretsManagerConfigSource extends AbstractAwsConfigSource {
    public SecretsManagerConfigSource(SecretsManagerClient client, Map<String, String> lookup, int ordinal) {
        this(client, lookup, ordinal, ConfigSourceFormat.AUTO);
    }

    public SecretsManagerConfigSource(SecretsManagerClient client, Map<String, String> lookup, int ordinal,
            ConfigSourceFormat format) {
        super("quarkus.secretsmanager.config", ordinal, new SecretsManagerConfigProvider(client, List.copyOf(lookup.values())),
                entries(lookup, format), Duration.ZERO, "quarkus-amazon-secretsmanager-config-poll");
    }

    private static Function<ResourceId, List<ConfigSourceEntry>> entries(Map<String, String> lookup,
            ConfigSourceFormat format) {
        Map<String, String> mappings = Map.copyOf(lookup);
        return id -> {
            if (mappings.isEmpty()) {
                return List.of(new ConfigSourceEntry(id.value(), id, id.value(), format, false));
            }
            List<ConfigSourceEntry> entries = new ArrayList<>();
            mappings.forEach((prefix, secretId) -> {
                if (secretId.equals(id.value())) {
                    entries.add(new ConfigSourceEntry(prefix, id, prefix, format, false));
                }
            });
            return entries;
        };
    }
}
