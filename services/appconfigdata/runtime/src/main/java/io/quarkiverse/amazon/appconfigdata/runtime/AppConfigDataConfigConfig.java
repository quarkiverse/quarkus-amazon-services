package io.quarkiverse.amazon.appconfigdata.runtime;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import io.quarkiverse.amazon.common.runtime.ConfigSourceFormat;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.DurationConverter;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.appconfigdata.config")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface AppConfigDataConfigConfig {
    /**
     * Whether to enable the AWS AppConfig Data ConfigSource ({@code quarkus.appconfigdata.config}).
     * <p>
     * When disabled, no configuration values are loaded from AppConfig Data.
     */
    @WithDefault("false")
    boolean enabled();

    /** Fail startup if configuration cannot be loaded completely, including missing resources, access or decoding errors. */
    @WithDefault("true")
    boolean failOnMissingConfig();

    /** Config source ordinal; higher values take precedence. Defaults between .env and application.properties. */
    @WithDefault("270")
    int ordinal();

    /** Named profiles, combined with the legacy application/environment/configuration-profile selection. */
    Map<String, ProfileEntry> entries();

    @ConfigGroup
    interface ProfileEntry {
        /** AppConfig application ID or name. */
        String application();

        /** AppConfig environment ID or name. */
        String environment();

        /** AppConfig configuration profile ID or name. */
        String configurationProfile();

        /** Optional target property/prefix. Allows raw text and non-null JSON scalars when non-empty. */
        Optional<String> prefix();

        /** Document format; defaults to the source-wide format. */
        Optional<ConfigSourceFormat> format();

        /** Session minimum from 15 to 86400 seconds; defaults to the source-wide setting, or 60 seconds. */
        Optional<Integer> requiredMinimumPollIntervalInSeconds();
    }

    /**
     * AppConfig application ID or name for
     * {@link software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionRequest}.
     */
    Optional<String> application();

    /**
     * AppConfig environment ID or name for
     * {@link software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionRequest}.
     */
    Optional<String> environment();

    /**
     * AppConfig configuration profile ID or name for
     * {@link software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionRequest}.
     */
    Optional<String> configurationProfile();

    /**
     * Format of the configuration document. Auto detects JSON objects/arrays, then YAML by Content-Type,
     * otherwise using Java properties. YAML requires the quarkus-config-yaml extension.
     * To use raw values or non-null JSON scalars, configure a named entry with a non-empty prefix.
     */
    @WithDefault("auto")
    ConfigSourceFormat format();

    /**
     * Minimum interval between {@code GetLatestConfiguration} calls for the session, from 15 to 86400 seconds.
     * Defaults to 60 seconds when absent; the server may require a longer interval.
     */
    Optional<Integer> requiredMinimumPollIntervalInSeconds();

    /**
     * Interval in minutes between polling for configuration updates after the initial fetch.
     * <p>
     * Set greater than {@code 0} to enable periodic polling.
     */
    @Deprecated(forRemoval = true)
    @WithDefault("0")
    int updateIntervalMinutes();

    /**
     * Minimum delay after a completed read before polling again. The server's minimum may extend this delay.
     * <p>
     * Set to or greater than {@code PT1S} to enable periodic polling.
     */
    @WithDefault("PT0S")
    @WithConverter(DurationConverter.class)
    Duration updateInterval();

}
