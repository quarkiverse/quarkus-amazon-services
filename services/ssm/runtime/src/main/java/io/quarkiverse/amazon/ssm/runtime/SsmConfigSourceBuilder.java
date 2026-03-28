package io.quarkiverse.amazon.ssm.runtime;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

public class SsmConfigSourceBuilder implements ConfigBuilder {

    @Override
    public SmallRyeConfigBuilder configBuilder(SmallRyeConfigBuilder builder) {
        return builder.withSources(new SsmConfigSourceFactory());
    }
}
