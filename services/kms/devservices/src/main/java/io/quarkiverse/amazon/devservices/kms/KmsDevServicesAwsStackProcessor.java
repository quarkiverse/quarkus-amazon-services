package io.quarkiverse.amazon.devservices.kms;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesAwsStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.AwsStackContainer;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.kms.runtime.KmsBuildTimeConfig;
import io.quarkiverse.amazon.kms.runtime.KmsDevServicesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.NotFoundException;

public class KmsDevServicesAwsStackProcessor extends AbstractDevServicesAwsStackProcessor {

    @BuildStep
    DevServicesAwsStackProviderBuildItem setupKms(KmsBuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup("kms", clientBuildTimeConfig.devservices(), globalConfig);
    }

    @Override
    protected void prepareAwsStackContainer(DevServicesBuildTimeConfig clientBuildTimeConfig, AwsStackContainer localstack) {
        createKeyAliases(localstack, (KmsDevServicesBuildTimeConfig) clientBuildTimeConfig);
    }

    public void createKeyAliases(AwsStackContainer awsStackContainer, KmsDevServicesBuildTimeConfig configuration) {

        configuration.keyAliases().ifPresent(keys -> {

            try (KmsClient client = awsStackContainer.configure(KmsClient.builder())
                    .httpClientBuilder(UrlConnectionHttpClient.builder())
                    .build()) {
                for (var keyName : keys) {
                    try {
                        client.describeKey(k -> k.keyId(keyName));
                        // the alias already exists in a shared container
                        continue;
                    } catch (NotFoundException e) {
                        // create the key and its alias
                    }
                    String masterKeyId = client.createKey().keyMetadata().keyId();
                    client.createAlias(t -> t.aliasName(keyName).targetKeyId(masterKeyId));
                }
            }
        });
    }
}
