package io.quarkiverse.amazon.sqs.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface SqsDevServicesBuildTimeConfig extends DevServicesBuildTimeConfig {

    /**
     * The queues to create on startup.
     */
    Optional<List<String>> queues();

}
