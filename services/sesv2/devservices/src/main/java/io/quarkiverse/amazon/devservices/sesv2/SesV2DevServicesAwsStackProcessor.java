package io.quarkiverse.amazon.devservices.sesv2;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesAwsStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.AwsStackContainer;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.sesv2.runtime.SesV2BuildTimeConfig;
import io.quarkiverse.amazon.sesv2.runtime.SesV2DevServicesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.AlreadyExistsException;

public class SesV2DevServicesAwsStackProcessor extends AbstractDevServicesAwsStackProcessor {

    @BuildStep
    DevServicesAwsStackProviderBuildItem setupSesV2(SesV2BuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup("sesv2", clientBuildTimeConfig.devservices(), globalConfig);
    }

    @Override
    protected void prepareAwsStackContainer(DevServicesBuildTimeConfig clientBuildTimeConfig, AwsStackContainer localstack) {
        createEmailIdentities(localstack, (SesV2DevServicesBuildTimeConfig) clientBuildTimeConfig);
    }

    public void createEmailIdentities(AwsStackContainer awsStackContainer, SesV2DevServicesBuildTimeConfig configuration) {

        configuration.verifiedEmails().ifPresent(emails -> {

            try (SesV2Client client = awsStackContainer.configure(SesV2Client.builder())
                    .httpClientBuilder(UrlConnectionHttpClient.builder())
                    .build()) {
                for (var email : emails) {
                    try {
                        client.createEmailIdentity(i -> i.emailIdentity(email));
                    } catch (AlreadyExistsException e) {
                        // the identity is already verified, for example when SES and SES V2 share the same container
                    }
                }
            }
        });
    }
}
