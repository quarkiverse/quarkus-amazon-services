package io.quarkiverse.amazon.appconfigdata.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkiverse.amazon.appconfigdata.runtime.AppConfigDataProvider.Profile;
import io.quarkiverse.amazon.common.config.AbstractAwsConfigSource;
import io.quarkiverse.amazon.common.config.ConfigSourceEntry;
import io.quarkiverse.amazon.common.runtime.ConfigSourceFormat;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;

/** AppConfig documents published as one complete snapshot. Owns the supplied client. */
public class AppConfigDataConfigSource extends AbstractAwsConfigSource {
    private static final Logger LOG = Logger.getLogger(AppConfigDataConfigSource.class);

    public AppConfigDataConfigSource(AppConfigDataClient client, String application, String environment, String profile,
            Integer requiredMinimumPollIntervalInSeconds, Duration updateInterval, int ordinal) {
        this(client, application, environment, profile, requiredMinimumPollIntervalInSeconds, updateInterval, ordinal,
                ConfigSourceFormat.AUTO);
    }

    public AppConfigDataConfigSource(AppConfigDataClient client, String application, String environment, String profile,
            Integer requiredMinimumPollIntervalInSeconds, Duration updateInterval, int ordinal, ConfigSourceFormat format) {
        this(client, profiles(application, environment, profile, requiredMinimumPollIntervalInSeconds), updateInterval, ordinal,
                format);
    }

    public AppConfigDataConfigSource(AppConfigDataClient client, List<Profile> profiles, Duration updateInterval, int ordinal,
            ConfigSourceFormat format) {
        super("quarkus.appconfigdata.config", ordinal, new AppConfigDataProvider(client, profiles),
                id -> List.of(new ConfigSourceEntry(id.value(), id, "", format, true)),
                profiles.isEmpty() ? Duration.ZERO : updateInterval, "quarkus-amazon-appconfigdata-config-poll");
    }

    static List<Profile> profiles(String application, String environment, String profile, Integer minimum) {
        if (application == null || application.isBlank() || environment == null || environment.isBlank()
                || profile == null || profile.isBlank()) {
            LOG.error("AppConfig Data ConfigSource requires application, environment and configuration-profile identifiers.");
            return List.of();
        }
        return List.of(new Profile(application, environment, profile, minimum));
    }

    void fetchConfig() {
        refresh();
    }

    static Map<String, String> parseConfiguration(String content) {
        return parseConfiguration(content, null);
    }

    static Map<String, String> parseConfiguration(String content, String contentType) {
        return ConfigSourceEntry.decodeDocument("", content, contentType);
    }
}
