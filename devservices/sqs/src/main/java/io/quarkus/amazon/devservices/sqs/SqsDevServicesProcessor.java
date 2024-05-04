package io.quarkus.amazon.devservices.sqs;

import static java.util.Collections.emptyList;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;

import io.quarkus.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkus.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkus.amazon.common.deployment.spi.LocalStackDevServicesBaseConfig;
import io.quarkus.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkus.amazon.sqs.runtime.SqsBuildTimeConfig;
import io.quarkus.amazon.sqs.runtime.SqsDevServicesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

public class SqsDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupSqs(SqsBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(Service.SQS, clientBuildTimeConfig.devservices());
    }

    @Override
    protected void prepareLocalStack(DevServicesBuildTimeConfig clientBuildTimeConfig, LocalStackContainer localstack) {
        createQueues(localstack, getConfiguration((SqsDevServicesBuildTimeConfig) clientBuildTimeConfig));
    }

    public void createQueues(LocalStackContainer localstack, SqsDevServiceCfg configuration) {
        try (SqsClient client = SqsClient.builder()
                .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.SQS))
                .region(Region.of(localstack.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials
                        .create(localstack.getAccessKey(), localstack.getSecretKey())))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build()) {
            for (var queueName : configuration.queues) {
                client.createQueue(b -> b.queueName(queueName));
            }
        }
    }

    private SqsDevServiceCfg getConfiguration(SqsDevServicesBuildTimeConfig devServicesConfig) {
        return new SqsDevServiceCfg(devServicesConfig);
    }

    private static final class SqsDevServiceCfg extends LocalStackDevServicesBaseConfig {
        private final Set<String> queues;

        public SqsDevServiceCfg(SqsDevServicesBuildTimeConfig config) {
            super(config.shared(), config.isolated(), config.serviceName(), config.containerProperties());
            this.queues = new HashSet<>(config.queues().orElse(emptyList()));
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o) && Objects.equals(queues, ((SqsDevServiceCfg) o).queues);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), queues);
        }
    }
}
