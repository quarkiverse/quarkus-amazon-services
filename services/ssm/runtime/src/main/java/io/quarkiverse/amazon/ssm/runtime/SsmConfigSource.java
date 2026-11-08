package io.quarkiverse.amazon.ssm.runtime;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.quarkiverse.amazon.common.config.AbstractAwsConfigSource;
import io.quarkiverse.amazon.common.config.ConfigSourceEntry;
import io.quarkiverse.amazon.common.config.ResourceId;
import io.quarkiverse.amazon.common.runtime.ConfigSourceFormat;
import io.quarkiverse.amazon.ssm.runtime.SsmConfigConfig.KeyNaming;
import io.quarkiverse.amazon.ssm.runtime.SsmConfigProvider.Path;
import software.amazon.awssdk.services.ssm.SsmClient;

/** Parameter Store config source with complete snapshot publication and optional polling. Owns the supplied client. */
public class SsmConfigSource extends AbstractAwsConfigSource {
    public record Selection(String name, Path path, String prefix, ConfigSourceFormat format) {
    }

    public SsmConfigSource(SsmClient client, String path, List<String> names, boolean recursive, boolean withDecryption,
            Duration updateInterval, int ordinal) {
        this(client, path, names, recursive, withDecryption, updateInterval, ordinal, ConfigSourceFormat.AUTO);
    }

    public SsmConfigSource(SsmClient client, String path, List<String> names, boolean recursive, boolean withDecryption,
            Duration updateInterval, int ordinal, ConfigSourceFormat format) {
        this(client, path, names, recursive, withDecryption, updateInterval, ordinal, format, false);
    }

    public SsmConfigSource(SsmClient client, String path, List<String> names, boolean recursive, boolean withDecryption,
            Duration updateInterval, int ordinal, ConfigSourceFormat format, boolean failOnMissingConfig) {
        this(client, path == null || path.isBlank() ? List.of()
                : List.of(new Selection("path", new Path(path, recursive), "", format)), names, withDecryption,
                updateInterval, ordinal, format, KeyNaming.FULL, failOnMissingConfig);
    }

    public SsmConfigSource(SsmClient client, List<Selection> paths, List<String> names, boolean withDecryption,
            Duration updateInterval, int ordinal, ConfigSourceFormat format, KeyNaming keyNaming, boolean failOnMissingConfig) {
        super("quarkus.ssm.config", ordinal,
                new SsmConfigProvider(client, paths.stream().map(Selection::path).toList(), names, withDecryption),
                entries(paths, names, format, keyNaming), updateInterval, "quarkus-amazon-ssm-config-poll",
                failOnMissingConfig);
    }

    private static Function<ResourceId, List<ConfigSourceEntry>> entries(List<Selection> paths, List<String> names,
            ConfigSourceFormat format, KeyNaming keyNaming) {
        var selections = List.copyOf(paths);
        var explicitNames = names == null ? List.<String> of()
                : names.stream().map(SsmConfigSource::canonicalName).distinct().toList();
        return id -> {
            var entries = new ArrayList<ConfigSourceEntry>();
            String name = id.value().split(":", 2)[0];
            if (!id.value().contains(":")) {
                for (Selection selection : selections) {
                    String path = selection.path().value();
                    String base = path.equals("/") ? path : path + "/";
                    if (name.startsWith(base)) {
                        String relative = name.substring(base.length());
                        if (selection.path().recursive() || !relative.contains("/")) {
                            String key = normalizeParameterName(keyNaming == KeyNaming.RELATIVE ? relative : name);
                            if (keyNaming == KeyNaming.RELATIVE) {
                                key = key.replaceAll("(^|\\.)_(\\d+)_(?=\\.|$)", "[$2]");
                            }
                            String prefix = selection.prefix().isEmpty() ? key
                                    : selection.prefix() + (key.startsWith("[") ? "" : ".") + key;
                            entries.add(new ConfigSourceEntry(selection.name(), id, prefix, selection.format(), false));
                        }
                    }
                }
            }
            if (explicitNames.contains(canonicalName(id.value()))) {
                String prefix = normalizeParameterName(name);
                if (entries.stream().noneMatch(entry -> entry.prefix().equals(prefix) && entry.format() == format)) {
                    entries.add(new ConfigSourceEntry("names:" + id.value(), id, prefix, format, false));
                }
            }
            return entries;
        };
    }

    private static String canonicalName(String name) {
        int arnPath = name.indexOf(":parameter/");
        String value = name.startsWith("arn:") && arnPath >= 0 ? name.substring(arnPath + ":parameter".length()) : name;
        return value.startsWith("/") ? value.substring(1) : value;
    }

    void reload() {
        refresh();
    }

    static String normalizeParameterName(String name) {
        if (name == null) {
            return "";
        }
        String normalized = name.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized.replace('/', '.');
    }

    static Map<String, String> expandParameterValue(String awsName, String value) {
        return ConfigSourceFormat.AUTO.decode(normalizeParameterName(awsName), value);
    }
}
