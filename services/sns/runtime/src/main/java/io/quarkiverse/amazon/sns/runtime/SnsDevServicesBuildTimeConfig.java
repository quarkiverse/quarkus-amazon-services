package io.quarkiverse.amazon.sns.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface SnsDevServicesBuildTimeConfig extends DevServicesBuildTimeConfig {

    /**
     * The topics to create on startup.
     */
    Optional<List<String>> topics();

}
