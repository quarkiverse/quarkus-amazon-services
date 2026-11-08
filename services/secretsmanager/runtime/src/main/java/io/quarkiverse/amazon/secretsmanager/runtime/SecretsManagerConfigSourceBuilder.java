package io.quarkiverse.amazon.secretsmanager.runtime;

import io.quarkiverse.amazon.common.runtime.AwsConfigExpression;
import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SecretKeysHandlerFactory.LazySecretKeysHandler;
import io.smallrye.config.SmallRyeConfigBuilder;

public class SecretsManagerConfigSourceBuilder implements ConfigBuilder {

    @Override
    public SmallRyeConfigBuilder configBuilder(SmallRyeConfigBuilder builder) {
        var expressions = new AwsConfigExpression("sm", "secretsmanager", SecretsManagerExpressionProvider::new);
        return builder.withSources(new SecretsManagerConfigSourceFactory()).withSources(expressions)
                .withSecretKeysHandlers(new LazySecretKeysHandler(expressions));
    }
}
