package io.quarkus.amazon.sqs.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class SqsDevServicesBuildTimeConfig extends DevServicesBuildTimeConfig {

    /**
     * The queues to create on startup.
     */
    @ConfigItem
    public Optional<List<String>> queues = Optional.empty();

}
