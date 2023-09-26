package io.quarkus.amazon.secretsmanager.config.runtime;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

public class SecretsManagerConfigSourceAsyncFactoryBuilder implements ConfigBuilder {
    @Override
    public SmallRyeConfigBuilder configBuilder(final SmallRyeConfigBuilder builder) {
        return builder.withSources(new SecretsManagerConfigSourceAsyncFactory());
    }
}