package io.quarkus.amazon.devservices.lambda;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;

import io.quarkus.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkus.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkus.amazon.common.deployment.spi.LocalStackDevServicesBaseConfig;
import io.quarkus.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkus.amazon.lambda.client.runtime.LambdaBuildTimeConfig;
import io.quarkus.amazon.lambda.client.runtime.LambdaDevServicesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.Runtime;

public class LambdaDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    public static final String HELLO_LAMBDA = "hello-lambda";

    @BuildStep
    @SuppressWarnings("unused")
    DevServicesLocalStackProviderBuildItem setupLambda(LambdaBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(Service.LAMBDA, clientBuildTimeConfig.devservices());
    }

    @Override
    protected void prepareLocalStack(DevServicesBuildTimeConfig clientBuildTimeConfig, LocalStackContainer localstack) {
        createFunctions(localstack, getConfiguration((LambdaDevServicesBuildTimeConfig) clientBuildTimeConfig));
    }

    @SuppressWarnings("unused")
    public void createFunctions(LocalStackContainer localstack, LambdaDevServiceCfg configuration) {
        try (LambdaClient client = LambdaClient.builder()
                .endpointOverride(localstack.getEndpointOverride(Service.LAMBDA))
                .region(Region.of(localstack.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials
                        .create(localstack.getAccessKey(), localstack.getSecretKey())))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build()) {
            client.createFunction(b -> b.functionName(HELLO_LAMBDA)
                    .runtime(Runtime.NODEJS18_X)
                    .handler("index.handler")
                    .role("arn:aws:iam::000000000000:role/lambda-role")
                    .code(c -> c.zipFile(SdkBytes
                            .fromInputStream(
                                    Optional.ofNullable(getClass().getResourceAsStream("/functions/hello-lambda.zip-archive"))
                                            .orElseThrow()))));
        }
    }

    private LambdaDevServiceCfg getConfiguration(LambdaDevServicesBuildTimeConfig devServicesConfig) {
        return new LambdaDevServiceCfg(devServicesConfig);
    }

    private static final class LambdaDevServiceCfg extends LocalStackDevServicesBaseConfig {

        private final Set<String> functions = Set.of(HELLO_LAMBDA);

        public LambdaDevServiceCfg(LambdaDevServicesBuildTimeConfig config) {
            super(config.shared(), config.serviceName(), config.containerProperties());
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o) && Objects.equals(functions, ((LambdaDevServiceCfg) o).functions);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(),
                    functions);
        }

    }
}
