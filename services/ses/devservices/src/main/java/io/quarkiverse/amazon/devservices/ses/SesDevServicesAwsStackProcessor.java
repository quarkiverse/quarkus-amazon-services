package io.quarkiverse.amazon.devservices.ses;

import io.quarkiverse.amazon.common.deployment.spi.AbstractDevServicesAwsStackProcessor;
import io.quarkiverse.amazon.common.deployment.spi.AwsStackContainer;
import io.quarkiverse.amazon.common.deployment.spi.DevServicesAwsStackProviderBuildItem;
import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.GlobalDevServicesBuildTimeConfig;
import io.quarkiverse.amazon.ses.runtime.SesBuildTimeConfig;
import io.quarkiverse.amazon.ses.runtime.SesDevServicesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildStep;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

public class SesDevServicesAwsStackProcessor extends AbstractDevServicesAwsStackProcessor {

    @BuildStep
    DevServicesAwsStackProviderBuildItem setupSes(SesBuildTimeConfig clientBuildTimeConfig,
            GlobalDevServicesBuildTimeConfig globalConfig) {
        return this.setup("ses", clientBuildTimeConfig.devservices(), globalConfig);
    }

    @Override
    protected void prepareAwsStackContainer(DevServicesBuildTimeConfig clientBuildTimeConfig, AwsStackContainer localstack) {
        verifyEmails(localstack, (SesDevServicesBuildTimeConfig) clientBuildTimeConfig);
    }

    public void verifyEmails(AwsStackContainer awsStackContainer, SesDevServicesBuildTimeConfig configuration) {

        configuration.verifiedEmails().ifPresent(emails -> {

            try (SesClient client = SesClient.builder()
                    .endpointOverride(awsStackContainer.getEndpoint())
                    .region(Region.of(awsStackContainer.getRegion()))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials
                            .create(awsStackContainer.getAccessKey(), awsStackContainer.getSecretKey())))
                    .httpClientBuilder(UrlConnectionHttpClient.builder())
                    .build()) {
                for (var emailAddress : emails) {
                    client.verifyEmailIdentity(e -> e.emailAddress(emailAddress));
                }
            }
        });
    }
}
