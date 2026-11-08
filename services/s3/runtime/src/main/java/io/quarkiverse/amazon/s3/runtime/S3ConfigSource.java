package io.quarkiverse.amazon.s3.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import io.quarkiverse.amazon.common.config.AbstractAwsConfigSource;
import io.quarkiverse.amazon.common.config.ConfigSourceEntry;
import io.quarkiverse.amazon.common.config.ResourceId;
import io.quarkiverse.amazon.common.runtime.ConfigSourceFormat;
import io.quarkiverse.amazon.s3.runtime.S3ConfigProvider.ObjectLocation;
import software.amazon.awssdk.services.s3.S3Client;

public class S3ConfigSource extends AbstractAwsConfigSource {
    public record Selection(String name, ObjectLocation object, String prefix, ConfigSourceFormat format) {
    }

    public S3ConfigSource(S3Client client, List<Selection> entries, int maxObjectSize, Duration updateInterval,
            int ordinal, boolean failOnMissingConfig) {
        this(new S3ConfigProvider(client, entries.stream().map(Selection::object).toList(), maxObjectSize),
                entries, updateInterval, ordinal, failOnMissingConfig);
    }

    private S3ConfigSource(S3ConfigProvider provider, List<Selection> entries, Duration updateInterval,
            int ordinal, boolean failOnMissingConfig) {
        super("quarkus.s3.config", ordinal, provider, entries(provider, entries),
                entries.stream().allMatch(entry -> entry.object().versionId() != null) ? Duration.ZERO : updateInterval,
                "quarkus-amazon-s3-config-poll", failOnMissingConfig);
    }

    private static Function<ResourceId, List<ConfigSourceEntry>> entries(S3ConfigProvider provider, List<Selection> entries) {
        var configured = List.copyOf(entries);
        return id -> configured.stream().filter(entry -> entry.object().resource().equals(id))
                .map(entry -> new ConfigSourceEntry(entry.name(), id, entry.prefix(),
                        entry.format() == ConfigSourceFormat.AUTO
                                ? detectFormat(provider.contentType(id), entry.object().key())
                                : entry.format(),
                        true))
                .toList();
    }

    static ConfigSourceFormat detectFormat(String contentType, String key) {
        String mime = contentType == null ? "" : contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (mime.equals("application/json") || mime.equals("text/json") || mime.endsWith("+json")) {
            return ConfigSourceFormat.JSON;
        }
        if (mime.equals("application/yaml") || mime.equals("application/x-yaml") || mime.equals("text/yaml")
                || mime.equals("text/x-yaml") || mime.endsWith("+yaml")) {
            return ConfigSourceFormat.YAML;
        }
        if (mime.equals("text/x-java-properties") || mime.equals("text/java-properties")
                || mime.equals("application/x-java-properties")) {
            return ConfigSourceFormat.PROPERTIES;
        }
        String lower = key.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".json")) {
            return ConfigSourceFormat.JSON;
        }
        if (lower.endsWith(".yaml") || lower.endsWith(".yml")) {
            return ConfigSourceFormat.YAML;
        }
        if (lower.endsWith(".properties")) {
            return ConfigSourceFormat.PROPERTIES;
        }
        throw new IllegalArgumentException("Unknown S3 configuration format; set an explicit entry format.");
    }
}
