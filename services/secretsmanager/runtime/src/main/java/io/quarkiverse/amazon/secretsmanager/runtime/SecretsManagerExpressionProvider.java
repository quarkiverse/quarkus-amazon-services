package io.quarkiverse.amazon.secretsmanager.runtime;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import io.quarkiverse.amazon.common.runtime.AwsConfigExpression;
import io.smallrye.config.ConfigSourceContext;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

final class SecretsManagerExpressionProvider implements AwsConfigExpression.ValueProvider {
    private final SecretsManagerClient client;

    SecretsManagerExpressionProvider(ConfigSourceContext context) {
        var factory = new SecretsManagerConfigSourceFactory();
        client = factory.build(factory.getClientConfig(context), context);
    }

    SecretsManagerExpressionProvider(SecretsManagerClient client) {
        this.client = client;
    }

    @Override
    public String fetch(String resource) throws Exception {
        var overrides = client.serviceClientConfiguration().overrideConfiguration();
        var response = client.getSecretValue(GetSecretValueRequest.builder().secretId(resource)
                .overrideConfiguration(request -> request
                        .apiCallTimeout(overrides.apiCallTimeout().orElse(Duration.ofSeconds(10)))
                        .apiCallAttemptTimeout(overrides.apiCallAttemptTimeout().orElse(Duration.ofSeconds(3))))
                .build());
        if (response.secretString() != null) {
            return response.secretString();
        }
        return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(response.secretBinary().asByteArray())).toString();
    }

    @Override
    public void close() {
        client.close();
    }
}
