package io.quarkiverse.amazon.secretsmanager.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
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

    /**
     * Mapping from a Quarkus configuration property name to an AWS Secrets Manager secret name.
     * <p>
     * - The map key is the configuration property to expose (for example {@code my.service.password}).<br>
     * - The map value is the Secrets Manager secret name to fetch and use as the property value.
     * <p>
     * If empty, all accessible secrets are loaded and exposed using the secret name as the configuration property name.
     */
    Map<String, String> secrets();

}
