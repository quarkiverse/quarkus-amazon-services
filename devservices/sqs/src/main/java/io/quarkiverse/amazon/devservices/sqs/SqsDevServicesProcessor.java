package io.quarkiverse.amazon.devservices.sqs;

import static java.util.Collections.emptyList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.testcontainers.containers.localstack.LocalStackContainer.Service;

import io.quarkiverse.amazon.common.deployment.spi.AwsStackContainer;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkiverse.amazon.common.deployment.spi.LegacyAbstractDevServicesLocalStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.LocalStackDevServicesBaseConfig;
import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.sqs.runtime.SqsBuildTimeConfig;
import io.quarkiverse.amazon.sqs.runtime.SqsDevServicesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

public class SqsDevServicesProcessor extends LegacyAbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupSqs(SqsBuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup(Service.SQS, clientBuildTimeConfig.devservices(), globalConfig);
    }

    @Override
    protected void prepareLocalStack(DevServicesBuildTimeConfig clientBuildTimeConfig, AwsStackContainer localstack) {
        createQueues(localstack, getConfiguration((SqsDevServicesBuildTimeConfig) clientBuildTimeConfig));
    }

    public void createQueues(AwsStackContainer localstack, SqsDevServiceCfg configuration) {
        try (SqsClient client = SqsClient.builder()
                .endpointOverride(localstack.getEndpoint())
                .region(Region.of(localstack.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials
                        .create(localstack.getAccessKey(), localstack.getSecretKey())))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build()) {
            for (var queueName : configuration.queues) {
                Map<QueueAttributeName, String> attributes = new HashMap<>();
                if (queueName.endsWith(".fifo")) {
                    attributes.put(QueueAttributeName.FIFO_QUEUE, Boolean.TRUE.toString());
                }
                client.createQueue(b -> b.queueName(queueName).attributes(attributes));
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
