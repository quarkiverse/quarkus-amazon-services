package io.quarkiverse.amazon.secretsmanager.runtime;

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

@ConfigMapping(prefix = "quarkus.secretsmanager.config")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface SecretsManagerConfigConfig {
    /**
     * Whether to enable the AWS Secrets Manager ConfigSource ({@code quarkus.secretsmanager.config}).
     * <p>
     * When disabled, no configuration values are loaded from AWS Secrets Manager.
     */
    @WithDefault("false")
    boolean enabled();

    /** Fail startup if configuration cannot be loaded completely, including missing resources, access or decoding errors. */
    @WithDefault("true")
    boolean failOnMissingConfig();

    /** Config source ordinal; higher values take precedence. Defaults between .env and application.properties. */
    @WithDefault("270")
    int ordinal();

    /** Minimum delay after a completed read before polling again. Zero disables polling; use at least one second. */
    @WithDefault("PT0S")
    @WithConverter(DurationConverter.class)
    Duration updateInterval();

    /**
     * Mapping from a Quarkus configuration property name to an AWS Secrets Manager secret name.
     * <p>
     * - The map key is the configuration property to expose (for example {@code my.service.password}).<br>
     * - The map value is the Secrets Manager secret name to fetch and use as the property value.
     * <p>
     * Combined with named entries. An empty selection never lists secrets or loads the account implicitly.
     */
    Map<String, String> secrets();

    /** Named secrets with independent prefixes and formats. */
    Map<String, SecretEntry> entries();

    @ConfigGroup
    interface SecretEntry {
        /** Secret name or ARN passed to GetSecretValue. */
        String secretId();

        /** Optional target property/prefix. Required for raw text and non-null JSON scalars. */
        Optional<String> prefix();

        /** Value format; defaults to the source-wide format. */
        Optional<ConfigSourceFormat> format();
    }

    /**
     * Format of each secret value. Auto detects JSON objects and arrays, otherwise preserving the raw value.
     * YAML requires the quarkus-config-yaml extension.
     */
    @WithDefault("auto")
    ConfigSourceFormat format();

}
