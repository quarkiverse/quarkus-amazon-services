package io.quarkus.amazon.secretsmanager.config.runtime;

import java.util.Map;

import io.quarkus.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface SecretsManagerDevServicesBuildTimeConfig extends DevServicesBuildTimeConfig {

    /**
     * The secrets to create on startup.
     */
    public Map<String, String> secrets();
}
