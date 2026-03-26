package io.quarkiverse.amazon.secretsmanager.runtime;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

public class SecretsManagerConfigSourceBuilder implements ConfigBuilder {

    @Override
    public SmallRyeConfigBuilder configBuilder(SmallRyeConfigBuilder builder) {
        return builder.withSources(new SecretsManagerConfigSourceFactory());
    }
}
