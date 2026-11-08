package io.quarkiverse.amazon.ssm.runtime;

import io.quarkiverse.amazon.common.runtime.AwsConfigExpression;
import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SecretKeysHandlerFactory.LazySecretKeysHandler;
import io.smallrye.config.SmallRyeConfigBuilder;

public class SsmConfigSourceBuilder implements ConfigBuilder {

    @Override
    public SmallRyeConfigBuilder configBuilder(SmallRyeConfigBuilder builder) {
        var expressions = new AwsConfigExpression("ssm", "ssm", SsmExpressionProvider::new);
        return builder.withSources(new SsmConfigSourceFactory()).withSources(expressions)
                .withSecretKeysHandlers(new LazySecretKeysHandler(expressions));
    }
}
