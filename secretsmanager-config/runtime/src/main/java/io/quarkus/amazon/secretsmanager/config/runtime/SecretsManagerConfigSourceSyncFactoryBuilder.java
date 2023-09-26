package io.quarkus.amazon.secretsmanager.config.runtime;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

public class SecretsManagerConfigSourceSyncFactoryBuilder implements ConfigBuilder {
    @Override
    public SmallRyeConfigBuilder configBuilder(final SmallRyeConfigBuilder builder) {
        return builder.withSources(new SecretsManagerConfigSourceSyncFactory());
    }
}