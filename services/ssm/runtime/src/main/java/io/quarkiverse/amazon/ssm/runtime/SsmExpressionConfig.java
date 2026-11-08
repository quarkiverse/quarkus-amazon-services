package io.quarkiverse.amazon.ssm.runtime;

import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.ssm.expression")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface SsmExpressionConfig {
    /** Successful expression reads are cached for this duration. Zero disables caching; negative durations are rejected. */
    @WithDefault("5M")
    Duration cacheTtl();
}
