package io.quarkiverse.amazon.ssm.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.quarkiverse.amazon.common.config.AbstractAwsConfigSource;
import io.quarkiverse.amazon.common.config.ConfigSourceEntry;
import io.quarkiverse.amazon.common.runtime.ConfigSourceFormat;
import software.amazon.awssdk.services.ssm.SsmClient;

/** Parameter Store config source with complete snapshot publication and optional polling. Owns the supplied client. */
public class SsmConfigSource extends AbstractAwsConfigSource {
    public SsmConfigSource(SsmClient client, String path, List<String> names, boolean recursive, boolean withDecryption,
            Duration updateInterval, int ordinal) {
        this(client, path, names, recursive, withDecryption, updateInterval, ordinal, ConfigSourceFormat.AUTO);
    }

    public SsmConfigSource(SsmClient client, String path, List<String> names, boolean recursive, boolean withDecryption,
            Duration updateInterval, int ordinal, ConfigSourceFormat format) {
        super("quarkus.ssm.config", ordinal, new SsmConfigProvider(client, path, names, recursive, withDecryption),
                id -> List.of(new ConfigSourceEntry(id.value(), id,
                        normalizeParameterName(id.value().split(":", 2)[0]), format, false)),
                updateInterval,
                "quarkus-amazon-ssm-config-poll");
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
