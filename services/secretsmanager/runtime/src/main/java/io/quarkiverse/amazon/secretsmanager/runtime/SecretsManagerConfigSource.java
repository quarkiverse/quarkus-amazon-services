package io.quarkiverse.amazon.secretsmanager.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.quarkiverse.amazon.common.config.AbstractAwsConfigSource;
import io.quarkiverse.amazon.common.config.ConfigSourceEntry;
import io.quarkiverse.amazon.common.config.ResourceId;
import io.quarkiverse.amazon.common.runtime.ConfigSourceFormat;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

/** Loads complete snapshots, fetching each mapped secret once per read. Owns the supplied client. */
public class SecretsManagerConfigSource extends AbstractAwsConfigSource {
    public SecretsManagerConfigSource(SecretsManagerClient client, Map<String, String> lookup, int ordinal) {
        this(client, lookup, ordinal, ConfigSourceFormat.AUTO);
    }

    public SecretsManagerConfigSource(SecretsManagerClient client, Map<String, String> lookup, int ordinal,
            ConfigSourceFormat format) {
        this(client, lookup, ordinal, format, false);
    }

    public SecretsManagerConfigSource(SecretsManagerClient client, Map<String, String> lookup, int ordinal,
            ConfigSourceFormat format, boolean failOnMissingConfig) {
        this(client, lookup.entrySet().stream().map(entry -> new ConfigSourceEntry(entry.getKey(),
                new ResourceId(entry.getValue()), entry.getKey(), format, false)).toList(), ordinal, failOnMissingConfig);
    }

    public SecretsManagerConfigSource(SecretsManagerClient client, List<ConfigSourceEntry> entries, int ordinal,
            boolean failOnMissingConfig) {
        this(client, entries, ordinal, failOnMissingConfig, false);
    }

    public SecretsManagerConfigSource(SecretsManagerClient client, List<ConfigSourceEntry> entries, int ordinal,
            boolean failOnMissingConfig, Duration updateInterval) {
        this(client, entries, ordinal, failOnMissingConfig, false, updateInterval);
    }

    public SecretsManagerConfigSource(SecretsManagerClient client, List<ConfigSourceEntry> entries, int ordinal,
            boolean failOnMissingConfig, boolean batchRetrieval) {
        this(client, entries, ordinal, failOnMissingConfig, batchRetrieval, Duration.ZERO);
    }

    public SecretsManagerConfigSource(SecretsManagerClient client, List<ConfigSourceEntry> entries, int ordinal,
            boolean failOnMissingConfig, boolean batchRetrieval, Duration updateInterval) {
        super("quarkus.secretsmanager.config", ordinal,
                new SecretsManagerConfigProvider(client, entries.stream().map(entry -> entry.resource().value()).toList(),
                        batchRetrieval),
                entries(entries), updateInterval,
                "quarkus-amazon-secretsmanager-config-poll", failOnMissingConfig);
    }

    private static Function<ResourceId, List<ConfigSourceEntry>> entries(List<ConfigSourceEntry> entries) {
        var configured = List.copyOf(entries);
        return id -> configured.stream().filter(entry -> entry.resource().equals(id)).toList();
    }
}
