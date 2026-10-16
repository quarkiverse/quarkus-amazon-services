package io.quarkiverse.amazon.devservices.sns;

import java.util.HashMap;
import java.util.Map;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesAwsStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.AwsStackContainer;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.sns.runtime.SnsBuildTimeConfig;
import io.quarkiverse.amazon.sns.runtime.SnsDevServicesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

public class SnsDevServicesAwsStackProcessor extends AbstractDevServicesAwsStackProcessor {

    @BuildStep
    DevServicesAwsStackProviderBuildItem setupSns(SnsBuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup("sns", clientBuildTimeConfig.devservices(), globalConfig);
    }

    @Override
    protected void prepareAwsStackContainer(DevServicesBuildTimeConfig clientBuildTimeConfig, AwsStackContainer localstack) {
        createTopics(localstack, (SnsDevServicesBuildTimeConfig) clientBuildTimeConfig);
    }

    public void createTopics(AwsStackContainer awsStackContainer, SnsDevServicesBuildTimeConfig configuration) {

        configuration.topics().ifPresent(topics -> {

            try (SnsClient client = SnsClient.builder()
                    .endpointOverride(awsStackContainer.getEndpoint())
                    .region(Region.of(awsStackContainer.getRegion()))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials
                            .create(awsStackContainer.getAccessKey(), awsStackContainer.getSecretKey())))
                    .httpClientBuilder(UrlConnectionHttpClient.builder())
                    .build()) {
                for (var topicName : topics) {
                    Map<String, String> attributes = new HashMap<>();
                    if (topicName.endsWith(".fifo")) {
                        attributes.put("FifoTopic", "true");
                    }
                    client.createTopic(t -> t.name(topicName).attributes(attributes));
                }
            }
        });
    }
}
