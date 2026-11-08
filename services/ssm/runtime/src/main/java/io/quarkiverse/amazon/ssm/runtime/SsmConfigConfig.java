package io.quarkiverse.amazon.ssm.runtime;

import java.time.Duration;
import java.util.List;
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
import io.smallrye.config.WithName;

/**
 * Runtime configuration for the Parameter Store {@link SsmConfigSource}.
 */
@ConfigMapping(prefix = "quarkus.ssm.config")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface SsmConfigConfig {

    /**
     * Whether to enable the AWS Systems Manager Parameter Store ConfigSource ({@code quarkus.ssm.config}).
     */
    @WithDefault("false")
    boolean enabled();

    /** Fail startup if configuration cannot be loaded completely, including missing resources, access or decoding errors. */
    @WithDefault("true")
    boolean failOnMissingConfig();

    /** Config source ordinal; higher values take precedence. Defaults between .env and application.properties. */
    @WithDefault("270")
    int ordinal();

    /**
     * Parameter Store path passed to {@link software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest}.
     * When set, all parameters under this path are loaded (subject to {@link #recursive()}).
     */
    Optional<String> path();

    /** Additional paths keyed by their configuration prefix, alongside the legacy scalar path. */
    @WithName("path")
    Map<String, String> prefixedPaths();

    /** Named paths with independent prefixes and formats. */
    Map<String, PathEntry> entries();

    /**
     * Path key naming. Relative is the default; full preserves the complete parameter name. Explicit names always use full
     * names.
     */
    @WithDefault("relative")
    KeyNaming keyNaming();

    enum KeyNaming {
        RELATIVE,
        /** Preserve keys that include the loaded path. */
        FULL
    }

    @ConfigGroup
    interface PathEntry {
        /** Parameter Store hierarchy to load. */
        String path();

        /** Optional prefix prepended to the exposed keys. */
        Optional<String> prefix();

        /** Value format; defaults to the source-wide format. */
        Optional<ConfigSourceFormat> format();

        /** Load nested parameters; defaults to the source-wide recursive setting. */
        Optional<Boolean> recursive();
    }

    /**
     * Individual parameter names (full paths, for example {@code /myapp/prod/db/url}) passed to
     * {@link software.amazon.awssdk.services.ssm.model.GetParametersRequest}. Batched in groups of ten per AWS limit.
     * <p>
     * You can combine this with {@link #path()}; the merged result is exposed as configuration keys.
     */
    Optional<List<String>> names();

    /**
     * Format of each parameter value. Auto detects JSON objects and arrays, otherwise preserving the raw value.
     * Use properties for Java properties documents; equals signs alone never select a format.
     * YAML requires the quarkus-config-yaml extension.
     */
    @WithDefault("auto")
    ConfigSourceFormat format();

    /**
     * When using {@link #path()}, whether to load parameters in sub-paths recursively.
     */
    @WithDefault("true")
    boolean recursive();

    /**
     * Whether to decrypt {@code SecureString} parameters when fetching values.
     */
    @WithDefault("true")
    boolean withDecryption();

    /**
     * Interval in minutes between reloads after the initial fetch. Set greater than {@code 0} to enable polling.
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
    @WithConverter(DurationConverter.class)
    Duration updateInterval();
}
