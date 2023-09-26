package io.quarkus.amazon.secretsmanager.config.runtime;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.quarkus.amazon.common.runtime.AsyncHttpClientConfig;
import io.quarkus.amazon.common.runtime.AwsConfig;
import io.quarkus.amazon.common.runtime.SdkConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientConfig;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.secretsmanager-config")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface SecretsManagerConfigConfig {
    /**
     * AWS SDK client configurations
     */
    @WithParentName
    @ConfigDocSection
    SdkConfig sdk();

    /**
     * AWS services configurations
     */
    @ConfigDocSection
    AwsConfig aws();

    /**
     * Sync HTTP transport configurations
     */
    @ConfigDocSection
    SyncHttpClientConfig syncClient();

    /**
     * Netty HTTP transport configurations
     */
    @ConfigDocSection
    AsyncHttpClientConfig asyncClient();

    /**
     * Allows you to add filters when listing secrets
     */
    FilterConfig filter();

    /**
     * Secrets matching those filter criteria will have their key prefixed
     */
    @WithName("filter")
    @ConfigDocMapKey("prefix")
    public Map<String, FilterConfig> filterPrefix();

    /**
     * Allows you to add filters when listing secrets
     */
    @ConfigGroup
    public interface FilterConfig {

        /**
         * enable this filter criterion
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * description: Prefix match, not case-sensitive.
         */
        Optional<Set<String>> description();

        /**
         * name: Prefix match, case-sensitive.
         */
        Optional<Set<String>> namePrefix();

        /**
         * tag-key: Prefix match, case-sensitive.
         */
        Optional<Set<String>> tagKey();

        /**
         * tag-value: Prefix match, case-sensitive.
         */
        Optional<Set<String>> tagValue();

        /**
         * primary-region: Prefix match, case-sensitive.
         */
        Optional<Set<String>> primaryRegionPrefix();

        /**
         * owning-service: Prefix match, case-sensitive.
         */
        Optional<Set<String>> owningServicePrefix();

        /**
         * all
         */
        Optional<Set<String>> all();
    }
}
