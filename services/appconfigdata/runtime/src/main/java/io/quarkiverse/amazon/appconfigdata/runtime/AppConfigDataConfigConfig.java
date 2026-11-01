package io.quarkiverse.amazon.appconfigdata.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
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
     * Optional minimum interval between {@code GetLatestConfiguration} calls for the session.
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
     * Interval between polling for configuration updates after the initial fetch.
     * <p>
     * Set to or greater than {@code PT1S} to enable periodic polling.
     */
    @WithDefault("PT0S")
    Duration updateInterval();

}
