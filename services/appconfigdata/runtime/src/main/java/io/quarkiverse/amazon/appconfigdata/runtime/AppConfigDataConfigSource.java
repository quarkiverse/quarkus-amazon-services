package io.quarkiverse.amazon.appconfigdata.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jboss.logging.Logger;

import io.quarkiverse.amazon.appconfigdata.runtime.AppConfigDataProvider.Profile;
import io.quarkiverse.amazon.common.config.AbstractAwsConfigSource;
import io.quarkiverse.amazon.common.config.ConfigSourceEntry;
import io.quarkiverse.amazon.common.config.ResourceId;
import io.quarkiverse.amazon.common.runtime.ConfigSourceFormat;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;

/** AppConfig documents published as one complete snapshot. Owns the supplied client. */
public class AppConfigDataConfigSource extends AbstractAwsConfigSource {
    public record Selection(String name, Profile profile, String prefix, ConfigSourceFormat format) {
    }

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
        this(client, profiles, updateInterval, ordinal, format, false);
    }

    public AppConfigDataConfigSource(AppConfigDataClient client, List<Profile> profiles, Duration updateInterval, int ordinal,
            ConfigSourceFormat format, boolean failOnMissingConfig) {
        this(client, profiles.stream().map(profile -> new Selection(profile.id().value(), profile, "", format)).toList(),
                updateInterval, ordinal, failOnMissingConfig);
    }

    public AppConfigDataConfigSource(AppConfigDataClient client, List<Selection> selections, Duration updateInterval,
            int ordinal,
            boolean failOnMissingConfig) {
        super("quarkus.appconfigdata.config", ordinal,
                new AppConfigDataProvider(client, selections.stream().map(Selection::profile).toList()), entries(selections),
                selections.isEmpty() ? Duration.ZERO : updateInterval, "quarkus-amazon-appconfigdata-config-poll",
                failOnMissingConfig);
    }

    private static Function<ResourceId, List<ConfigSourceEntry>> entries(List<Selection> selections) {
        var configured = List.copyOf(selections);
        return id -> configured.stream().filter(selection -> selection.profile().id().equals(id))
                .map(selection -> new ConfigSourceEntry(selection.name(), id, selection.prefix(), selection.format(), true))
                .toList();
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
