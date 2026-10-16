package io.quarkiverse.amazon.devservices.kms;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesAwsStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.AwsStackContainer;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.kms.runtime.KmsBuildTimeConfig;
import io.quarkiverse.amazon.kms.runtime.KmsDevServicesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;

public class KmsDevServicesAwsStackProcessor extends AbstractDevServicesAwsStackProcessor {

    @BuildStep
    DevServicesAwsStackProviderBuildItem setupKms(KmsBuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup("kms", clientBuildTimeConfig.devservices(), globalConfig);
    }

    @Override
    protected void prepareAwsStackContainer(DevServicesBuildTimeConfig clientBuildTimeConfig, AwsStackContainer localstack) {
        createTopics(localstack, (KmsDevServicesBuildTimeConfig) clientBuildTimeConfig);
    }

    public void createTopics(AwsStackContainer awsStackContainer, KmsDevServicesBuildTimeConfig configuration) {

        configuration.keyAliases().ifPresent(keys -> {

            try (KmsClient client = KmsClient.builder()
                    .endpointOverride(awsStackContainer.getEndpoint())
                    .region(Region.of(awsStackContainer.getRegion()))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials
                            .create(awsStackContainer.getAccessKey(), awsStackContainer.getSecretKey())))
                    .httpClientBuilder(UrlConnectionHttpClient.builder())
                    .build()) {
                for (var keyName : keys) {
                    String masterKeyId = client.createKey().keyMetadata().keyId();
                    client.createAlias(t -> t.aliasName(keyName).targetKeyId(masterKeyId));
                }
            }
        });
    }
}
