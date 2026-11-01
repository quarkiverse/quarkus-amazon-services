package io.quarkiverse.amazon.ssm.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

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

    /**
     * Parameter Store path passed to {@link software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest}.
     * When set, all parameters under this path are loaded (subject to {@link #recursive()}).
     */
    Optional<String> path();

    /**
     * Individual parameter names (full paths, for example {@code /myapp/prod/db/url}) passed to
     * {@link software.amazon.awssdk.services.ssm.model.GetParametersRequest}. Batched in groups of ten per AWS limit.
     * <p>
     * You can combine this with {@link #path()}; the merged result is exposed as configuration keys.
     */
    Optional<List<String>> names();

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
    Duration updateInterval();
}
