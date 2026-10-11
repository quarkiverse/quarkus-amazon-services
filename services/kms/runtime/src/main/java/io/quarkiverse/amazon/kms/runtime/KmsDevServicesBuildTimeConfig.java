package io.quarkiverse.amazon.kms.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface KmsDevServicesBuildTimeConfig extends DevServicesBuildTimeConfig {

    /**
     * The keys to create on startup.
     */
    Optional<List<String>> keyAliases();

}
