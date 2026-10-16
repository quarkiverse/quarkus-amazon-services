package io.quarkiverse.amazon.devservices.sqs;

import java.util.HashMap;
import java.util.Map;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesAwsStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.AwsStackContainer;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
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

public class SqsDevServicesAwsStackProcessor extends AbstractDevServicesAwsStackProcessor {

    @BuildStep
    DevServicesAwsStackProviderBuildItem setupSqs(SqsBuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup("sqs", clientBuildTimeConfig.devservices(), globalConfig);
    }

    @Override
    protected void prepareAwsStackContainer(DevServicesBuildTimeConfig clientBuildTimeConfig, AwsStackContainer localstack) {
        createQueues(localstack, (SqsDevServicesBuildTimeConfig) clientBuildTimeConfig);
    }

    public void createQueues(AwsStackContainer awsStackContainer, SqsDevServicesBuildTimeConfig configuration) {

        configuration.queues().ifPresent(queues -> {

            try (SqsClient client = SqsClient.builder()
                    .endpointOverride(awsStackContainer.getEndpoint())
                    .region(Region.of(awsStackContainer.getRegion()))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials
                            .create(awsStackContainer.getAccessKey(), awsStackContainer.getSecretKey())))
                    .httpClientBuilder(UrlConnectionHttpClient.builder())
                    .build()) {
                for (var queueName : queues) {
                    Map<QueueAttributeName, String> attributes = new HashMap<>();
                    if (queueName.endsWith(".fifo")) {
                        attributes.put(QueueAttributeName.FIFO_QUEUE, Boolean.TRUE.toString());
                    }
                    client.createQueue(b -> b.queueName(queueName).attributes(attributes));
                }
            }
        });
    }
}
