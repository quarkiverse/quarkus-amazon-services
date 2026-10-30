package io.quarkiverse.amazon.common.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import software.amazon.awssdk.regions.Region;

@ConfigGroup
public interface AwsConfig {

    /** Prefix of this AWS configuration group, used in credentials diagnostics. */
    default String configPrefix(String clientPrefix) {
        return clientPrefix + ".aws";
    }

    // @formatter:off
    /**
     * An Amazon Web Services region that hosts the given service.
     *
     * It overrides region provider chain with static value of
     * region with which the service client should communicate.
     *
     * If not set, region is retrieved via the default providers chain in the following order:
     *
     * * `aws.region` system property
     * * `region` property from the profile file
     * * Instance profile file
     *
     * See `software.amazon.awssdk.regions.Region` for available regions.
     *
     * @asciidoclet
     */
    // @formatter:on
    Optional<Region> region();

    /**
     * Defines credentials provider used
     */
    Optional<AwsCredentialsProviderConfig> credentials();

}
