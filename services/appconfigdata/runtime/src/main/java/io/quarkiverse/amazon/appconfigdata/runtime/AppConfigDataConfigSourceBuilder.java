package io.quarkiverse.amazon.appconfigdata.runtime;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

public class AppConfigDataConfigSourceBuilder implements ConfigBuilder {

    @Override
    public SmallRyeConfigBuilder configBuilder(SmallRyeConfigBuilder builder) {
        return builder.withSources(new AppConfigDataConfigSourceFactory());
    }
}
