package io.quarkiverse.amazon.s3.runtime;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import io.quarkiverse.amazon.common.runtime.ConfigSourceFormat;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.DurationConverter;
import io.quarkus.runtime.configuration.MemorySize;
import io.quarkus.runtime.configuration.MemorySizeConverter;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.s3.config")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface S3ConfigSourceConfig {
    /** Enable loading configuration from explicitly selected S3 objects. No bucket listing is performed. */
    @WithDefault("false")
    boolean enabled();

    /** Fail startup unless every selected object can be fetched and decoded into a complete, collision-free snapshot. */
    @WithDefault("true")
    boolean failOnMissingConfig();

    /** Config source ordinal; defaults between application.properties and .env. */
    @WithDefault("270")
    int ordinal();

    /** Default document format for entries. Auto uses a recognized Content-Type, then the object key's extension. */
    @WithDefault("auto")
    ConfigSourceFormat format();

    /** Polling interval for unpinned objects. Zero disables polling; positive values must be at least one second. */
    @WithDefault("PT0S")
    @WithConverter(DurationConverter.class)
    Duration updateInterval();

    /** Maximum bytes per object, enforced while reading. Must be positive and less than 2 GiB. */
    @WithDefault("1M")
    @WithConverter(MemorySizeConverter.class)
    MemorySize maxObjectSize();

    /** Named objects, each with independent format and property prefix. Duplicate remote identities share one read. */
    Map<String, ObjectEntry> entries();

    @ConfigGroup
    interface ObjectEntry {
        /** Bucket name or access point identifier accepted by the S3 SDK. */
        String bucket();

        /** Exact object key, without URL encoding or a leading slash unless it is part of the key. */
        String key();

        /**
         * Optional immutable version to read. The mutable S3 null version is rejected. Successfully loaded pins are not polled.
         */
        Optional<String> versionId();

        /** Optional property prefix, joined to decoded document keys with a dot. Required for raw or scalar values. */
        Optional<String> prefix();

        /**
         * Format override; defaults to the source-wide format. Auto uses a recognized Content-Type, then the object's
         * extension;
         * unknown formats fail. YAML requires quarkus-config-yaml.
         */
        Optional<ConfigSourceFormat> format();
    }
}
