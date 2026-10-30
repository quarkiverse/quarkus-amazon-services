package io.quarkiverse.amazon.sdk.runtime;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

import io.quarkiverse.amazon.common.runtime.AmazonClientConfig;
import io.quarkiverse.amazon.common.runtime.AwsConfig;
import io.quarkiverse.amazon.common.runtime.AwsCredentialsProviderConfig;
import io.quarkiverse.amazon.common.runtime.SdkConfig;
import software.amazon.awssdk.regions.Region;

final class ResolvedClientConfig implements AmazonClientConfig {
    private final AmazonClientConfig named;
    private final AmazonClientConfig defaults;

    ResolvedClientConfig(AmazonClientConfig named, AmazonClientConfig defaults) {
        this.named = named;
        this.defaults = defaults;
    }

    private <T> Optional<T> first(Function<AmazonClientConfig, Optional<T>> value) {
        return (named == null ? Optional.<T> empty() : value.apply(named)).or(() -> value.apply(defaults));
    }

    private String sourcePrefix(Function<AmazonClientConfig, ? extends Optional<?>> value, String namedPrefix) {
        return named != null && value.apply(named).isPresent() ? namedPrefix : "quarkus.aws";
    }

    @Override
    public AwsConfig aws() {
        return new AwsConfig() {
            @Override
            public String configPrefix(String clientPrefix) {
                return sourcePrefix(c -> c.aws().credentials(), clientPrefix);
            }

            @Override
            public Optional<Region> region() {
                return first(c -> c.aws().region());
            }

            @Override
            public Optional<AwsCredentialsProviderConfig> credentials() {
                // Credentials are inherited as a group, never mixed between identities.
                return first(c -> c.aws().credentials());
            }
        };
    }

    @Override
    public SdkConfig sdk() {
        return new SdkConfig() {
            @Override
            public String endpointOverrideConfigPrefix(String clientPrefix) {
                return sourcePrefix(c -> c.sdk().endpointOverride(), clientPrefix);
            }

            @Override
            public Optional<URI> endpointOverride() {
                return first(c -> c.sdk().endpointOverride());
            }

            @Override
            public Optional<Duration> apiCallTimeout() {
                return first(c -> c.sdk().apiCallTimeout());
            }

            @Override
            public Optional<Duration> apiCallAttemptTimeout() {
                return first(c -> c.sdk().apiCallAttemptTimeout());
            }

            @Override
            public Advanced advanced() {
                return () -> first(c -> c.sdk().advanced().useQuarkusScheduledExecutorService());
            }
        };
    }
}
