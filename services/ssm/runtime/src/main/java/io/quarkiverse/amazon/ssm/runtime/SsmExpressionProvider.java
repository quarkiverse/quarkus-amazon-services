package io.quarkiverse.amazon.ssm.runtime;

import java.time.Duration;

import io.quarkiverse.amazon.common.runtime.AwsConfigExpression;
import io.smallrye.config.ConfigSourceContext;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;

final class SsmExpressionProvider implements AwsConfigExpression.ValueProvider {
    private final SsmClient client;

    SsmExpressionProvider(ConfigSourceContext context) {
        var factory = new SsmConfigSourceFactory();
        client = factory.build(factory.getClientConfig(context), context);
    }

    SsmExpressionProvider(SsmClient client) {
        this.client = client;
    }

    @Override
    public String fetch(String resource) {
        var overrides = client.serviceClientConfiguration().overrideConfiguration();
        return client.getParameter(GetParameterRequest.builder().name(resource).withDecryption(true)
                .overrideConfiguration(request -> request
                        .apiCallTimeout(overrides.apiCallTimeout().orElse(Duration.ofSeconds(10)))
                        .apiCallAttemptTimeout(overrides.apiCallAttemptTimeout().orElse(Duration.ofSeconds(3))))
                .build()).parameter().value();
    }

    @Override
    public void close() {
        client.close();
    }
}
