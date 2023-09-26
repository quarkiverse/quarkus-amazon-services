package io.quarkus.amazon.secretsmanager.config.deployment;

import java.util.Map;

import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;

import io.quarkus.amazon.common.deployment.spi.AbstractDevServicesLocalStackProcessor;
import io.quarkus.amazon.common.deployment.spi.DevServicesLocalStackProviderBuildItem;
import io.quarkus.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkus.amazon.secretsmanager.config.runtime.SecretsManagerConfigBuildTimeConfig;
import io.quarkus.amazon.secretsmanager.config.runtime.SecretsManagerDevServicesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

public class SecretsManagerConfigDevServicesProcessor extends AbstractDevServicesLocalStackProcessor {

    @BuildStep
    DevServicesLocalStackProviderBuildItem setupSecretsManager(SecretsManagerConfigBuildTimeConfig clientBuildTimeConfig) {
        return this.setup(Service.SECRETSMANAGER, clientBuildTimeConfig.devservices());
    }

    @Override
    protected void overrideDefaultConfig(Map<String, String> defaultConfig) {
        for (String key : defaultConfig.keySet().toArray(new String[0])) {
            defaultConfig.put(key.replace(Service.SECRETSMANAGER.getName(), Service.SECRETSMANAGER.getName() + "-config"),
                    defaultConfig.remove(key));
        }
    }

    @Override
    protected void prepareLocalStack(DevServicesBuildTimeConfig clientBuildTimeConfig,
            LocalStackContainer localstack) {
        createSecrets(localstack, (SecretsManagerDevServicesBuildTimeConfig) clientBuildTimeConfig);
    }

    public void createSecrets(LocalStackContainer localstack, SecretsManagerDevServicesBuildTimeConfig configuration) {
        try (SecretsManagerClient client = SecretsManagerClient.builder()
                .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
                .region(Region.of(localstack.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials
                        .create(localstack.getAccessKey(), localstack.getSecretKey())))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build()) {
            configuration.secrets().forEach((key, value) -> {
                client.createSecret(r -> r.name(key).secretString(value));
            });
        }
    }
}
